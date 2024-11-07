package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.config.Feature;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.DRC_ASYNC_RESPONSE;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.FDC_GLOBAL_UPDATE;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.FETCHED_FROM_MAAT;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.SENT_TO_DRC;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.UPDATED_IN_MAAT;

@RequiredArgsConstructor
@Service
@Slf4j
public class FdcService implements FileService {
    private static final String SERVICE_NAME = "FdcService";
    private static final URI DUPLICATE_TYPE = URI.create("https://laa-debt-collection.service.justice.gov.uk/problem-types#duplicate-id");
    public static final String REQUESTED_STATUS = "REQUESTED";
    private final FdcMapperUtils fdcMapperUtils;
    private final FdcClient fdcClient;
    private final DrcClient drcClient;
    private final ObjectMapper objectMapper;
    private final Feature feature;
    private final EventService eventService;
    private BigInteger batchId;

    @SuppressWarnings("squid:S2147")  // Duplicate code is catch blocks. However they cannot be merged, due to lacking
    // a shared superclass with .getStatusCode() Same with the throws, to avoid the compiler complaining about throwing
    // a generic "Exception" back up the chain.
    public Integer processFdcUpdate(UpdateLogFdcRequest updateLogFdcRequest) {
        try {
            int result;
            if (!feature.incomingIsolated()) {
                result = fdcClient.sendLogFdcProcessed(updateLogFdcRequest);
            } else {
                log.info("processFdcUpdate: Not calling MAAT API sendLogFdcProcessed() because `feature.incoming-isolated=true`");
                return 0; // avoid updating MAAT DB.
            }
            logFdcAsyncEvent(updateLogFdcRequest, OK);
            return result;
        } catch (WebClientResponseException e){
            logFdcAsyncEvent(updateLogFdcRequest, e.getStatusCode());
            throw e;
        } catch (HttpServerErrorException e ){
            logFdcAsyncEvent(updateLogFdcRequest, e.getStatusCode());
            throw e;
        } catch (MaatApiClientException  e) {
            logFdcAsyncEvent(updateLogFdcRequest, e.getStatusCode());
            throw e;
        }
    }

    private void logFdcAsyncEvent(UpdateLogFdcRequest updateLogFdcRequest, HttpStatusCode httpStatusCode){
        Fdc idHolder = new Fdc();
        idHolder.setId(BigInteger.valueOf(updateLogFdcRequest.getFdcId()));
        eventService.logFdc(DRC_ASYNC_RESPONSE, null, idHolder, httpStatusCode, updateLogFdcRequest.getErrorText());
    }

    @Timed(value = "laa_dces_drc_service_process_fdc_daily_files",
            description = "Time taken to process the daily FDC files from DRC and passing this for downstream processing.")
    public boolean processDailyFiles() {
        batchId = eventService.generateBatchId();
        List<Fdc> successfulFdcs = new ArrayList<>();
        Map<String,String> failedFdcs = new HashMap<>();
        int globalUpdateResult = callGlobalUpdate();
        List<Fdc> fdcList = getFdcList();
        sendFdcToDrc(fdcList, successfulFdcs, failedFdcs);
        // check numbers.
        logNumberDiscepancies(globalUpdateResult, fdcList.size(), successfulFdcs.size());
        return updateFdcAndCreateFile(successfulFdcs, failedFdcs) != null;
    }

    @SuppressWarnings("squid:S2147") // as per above. Needed until exception refactor.
    @Retry(name = SERVICE_NAME)
    void sendFdcToDrc(List<Fdc> fdcList, List<Fdc> successfulFdcs, Map<String,String> failedFdcs) {
        for (Fdc currentFdc : fdcList) {
            eventService.logFdc(FETCHED_FROM_MAAT, batchId, currentFdc, OK, null);
            int fdcId = currentFdc.getId().intValue();
            try {
                final var request = FdcReqForDrc.of(fdcId, currentFdc);
                if (!feature.outgoingIsolated()) {
                    drcClient.sendFdcReqToDrc(request);
                    log.info("Sent FDC data to DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
                } else {
                    log.info("Skipping FDC data to DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
                    final var json = objectMapper.writeValueAsString(request);
                    log.debug("Skipping FDC data to DRC, JSON = [{}]", json);
                }
                successfulFdcs.add(currentFdc);
                eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, OK, null);
            } catch (MaatApiClientException e){
                handleDrcSentError(e, e.getStatusCode(), currentFdc, failedFdcs);
            } catch (WebClientResponseException e){
                if (e.getStatusCode().isSameCodeAs(CONFLICT)) {
                    ProblemDetail problemDetail = e.getResponseBodyAs(ProblemDetail.class);
                    if (problemDetail != null && DUPLICATE_TYPE.equals(problemDetail.getType())) {
                        log.info("Ignoring duplicate FDC error response from DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
                        successfulFdcs.add(currentFdc);
                        eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, CONFLICT, null);
                        continue;
                    }
                }
                handleDrcSentError(e, e.getStatusCode(), currentFdc, failedFdcs);
            } catch (HttpServerErrorException e){
                handleDrcSentError(e, e.getStatusCode(), currentFdc, failedFdcs);
            } catch (JsonProcessingException e) {
                handleDrcSentError(e, INTERNAL_SERVER_ERROR, currentFdc, failedFdcs);
            }
        }
    }

    private void handleDrcSentError(Exception e, HttpStatusCode httpStatusCode, Fdc currentFdc, Map<String,String> failedFdcs) {
        // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
        failedFdcs.put(Integer.toString(currentFdc.getId().intValue()), e.getClass().getName() + ": " + e.getMessage());
        eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, httpStatusCode, e.getMessage());
    }

    @SuppressWarnings("squid:S2147") // as per above, due to difference in superclasses.
    private Integer updateFdcAndCreateFile(List<Fdc> successfulFdcs, Map<String, String> failedFdcs) {
        // If any contributions were sent, then finish off with updates and create the file:
        Integer contributionFileId = null;
        if (Objects.nonNull(successfulFdcs) && !successfulFdcs.isEmpty()) {
            // Construct other parameters for the "ATOMIC UPDATE" call.
            LocalDateTime dateGenerated = LocalDateTime.now();
            String fileName = fdcMapperUtils.generateFileName(dateGenerated);
            String ackXml = fdcMapperUtils.generateAckXML(fileName, dateGenerated.toLocalDate(), failedFdcs.size(), successfulFdcs.size());
            String xmlFile = fdcMapperUtils.generateFileXML(successfulFdcs);
            List<String> successfulIdList = successfulFdcs.stream()
                    .map(Fdc::getId)
                    .filter(Objects::nonNull)
                    .map(BigInteger::toString).toList();

            // Failed XML lines to be logged. Need to use this to set the ATOMIC UPDATE's ack field.
            if (!failedFdcs.isEmpty()) {
                log.info("Failed to send {} FDC contributions", failedFdcs.size());
            }
            // Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            try {
                contributionFileId = fdcUpdateRequest(xmlFile, successfulIdList, successfulIdList.size(), fileName, ackXml);
                // Explicitly log the FDC contribution IDs that were updated:
                log.info("Created FDC contribution-file ID {} from {} FDC contribution IDs [{}]", contributionFileId, successfulIdList.size(), String.join(", ", successfulIdList));
            } catch (MaatApiClientException e){
                handleFileCreationError(e, e.getStatusCode());
                throw e;
            } catch (WebClientResponseException e){
                handleFileCreationError(e, e.getStatusCode());
                throw e;
            } catch (HttpServerErrorException e){
                handleFileCreationError(e, e.getStatusCode());
                throw e;
            }
        }
        logMaatUpdateEvents(successfulFdcs, failedFdcs);
        return contributionFileId;
    }

    private void handleFileCreationError(Exception e, HttpStatusCode httpStatusCode) {
        // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
        log.error("Failed to create FDC contribution-file. Investigation needed. State of files will be out of sync! [" + e.getClass().getName() + "(" + e.getMessage() + ")]");
        // If failed, we want to handle this. As it will mean the whole process failed for current day.
        eventService.logFdc(UPDATED_IN_MAAT, batchId,null, httpStatusCode, e.getMessage());
    }

    public void logNumberDiscepancies(int globalUpdateCount, int getFdcCount, int successfullySentFdcCount) {
        if (globalUpdateCount != getFdcCount) {
            log.info("FDC contribution count discrepancy: {} affected by FDC global update, {} from getFdcs", globalUpdateCount, getFdcCount);
        }
        if (getFdcCount != successfullySentFdcCount) {
            log.info("FDC contribution count discrepancy: {} from getFdcs, {} successfully sent", getFdcCount, successfullySentFdcCount);
        }
    }

    @Retry(name = SERVICE_NAME)
    public List<Fdc> getFdcList() throws HttpServerErrorException{
        FdcContributionsResponse response;
        try {
            response = fdcClient.getFdcContributions(REQUESTED_STATUS);
        } catch (HttpServerErrorException e) {
            // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
            log.error("Failed to retrieve FDC contributions, after the FDC global update completed [" + e.getClass().getName() + "(" + e.getMessage() + ")]");
            eventService.logFdc(FETCHED_FROM_MAAT, batchId, null, e.getStatusCode(), e.getMessage());
            throw e;
        }
        List<Fdc> fdcList = new ArrayList<>();
        if (Objects.nonNull(response)
                && Objects.nonNull(response.getFdcContributions())) {
            List<FdcContributionEntry> fdcContributionEntryList = response.getFdcContributions();
            fdcList = fdcContributionEntryList.stream().map(fdcMapperUtils::mapFdcEntry).toList();
            String successfulPayload = "Fetched "+fdcList.size()+" fdc entries";
            eventService.logFdc(FETCHED_FROM_MAAT, batchId, null, OK, successfulPayload);
        }
        return fdcList;
    }

    @Retry(name = SERVICE_NAME)
    public FdcGlobalUpdateResponse callFdcGlobalUpdate(){
        try {
            if (!feature.outgoingIsolated()) {
                return fdcClient.executeFdcGlobalUpdate();
            } else {
                log.info("callFdcGlobalUpdate: Not calling MAAT API executeFdcGlobalUpdate() because `feature.outgoing-isolated=true`");
                return new FdcGlobalUpdateResponse(true, 0);
            }
        } catch (HttpServerErrorException e) {
            // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
            log.error("FDC global update threw an exception [" + e.getClass().getName() + "(" + e.getMessage() + ")]");
            throw e;
        }
    }

    private int callGlobalUpdate() {
        FdcGlobalUpdateResponse globalUpdateResponse;
        try {
            globalUpdateResponse = callFdcGlobalUpdate();
        } catch (Exception e) {
            return logGlobalUpdateFailure(e.getMessage());
        }
        // handle response
        if (!globalUpdateResponse.isSuccessful()) {
            return logGlobalUpdateFailure("endpoint response has successful==false");
        }
        int numUpdates = globalUpdateResponse.getNumberOfUpdates();
        eventService.logFdc(FDC_GLOBAL_UPDATE, batchId, null, OK, "Updated "+numUpdates+" fdc entries");
        return numUpdates;
    }

    private int logGlobalUpdateFailure(String errorMessage){
        // We've failed to do a global update.
        // TODO: Flag here for sentry
        String errorText = String.format("Failed to complete FDC global update [%s]", errorMessage);
        log.error(errorText);
        eventService.logFdc(FDC_GLOBAL_UPDATE, batchId, null, INTERNAL_SERVER_ERROR, errorText);
        // continue processing, as can still have data to deal with.
        return 0;
    }

    @Retry(name = SERVICE_NAME)
    public Integer fdcUpdateRequest(String xmlContent, List<String> fdcIdList, int numberOfRecords, String fileName, String fileAckXML) throws HttpServerErrorException {
        FdcUpdateRequest request = FdcUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .fdcIds(fdcIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        if (!feature.outgoingIsolated()) {
            return fdcClient.updateFdcs(request);
        } else {
            log.info("fdcUpdateRequest: Not calling MAAT API updateFdcs() because `feature.outgoing-isolated=true`");
            return 0;
        }
    }

    private void logMaatUpdateEvents(List<Fdc> successfulFdcs, Map<String, String> failedFdcs) {
        // log success and failure numbers.
        eventService.logFdc(UPDATED_IN_MAAT, batchId, null, OK, "Successfully Sent:"+ successfulFdcs.size());
        eventService.logFdc(UPDATED_IN_MAAT, batchId, null, (failedFdcs.size()>0?INTERNAL_SERVER_ERROR:OK), "Failed To Send:"+ failedFdcs.size());
        // insert row for each successfully updated fdc.
        for(Fdc currentFdc: successfulFdcs){
            eventService.logFdc(UPDATED_IN_MAAT, batchId, currentFdc, OK, null);
        }
    }
}
