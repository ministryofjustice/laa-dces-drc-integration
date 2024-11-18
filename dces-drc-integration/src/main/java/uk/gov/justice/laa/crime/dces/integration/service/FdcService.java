package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;
import uk.gov.justice.laa.crime.dces.integration.utils.FileServiceUtils;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    public static final String REQUESTED_STATUS = "REQUESTED";
    private final FdcMapperUtils fdcMapperUtils;
    private final FdcClient fdcClient;
    private final DrcClient drcClient;
    private final ObjectMapper objectMapper;
    private final FeatureProperties feature;
    private final EventService eventService;
    private BigInteger batchId;

    /**
     * Method which logs that a specific fdc has been processed by the Debt Recovery Company.
     * <ul>
     * <li>Will log a success by incrementing the successful count of the associated contribution file.</li>
     * <li>If error text is present, will instead log it to the MAAT DB as an error for the associated contribution file.</li>
     * <li>Logs details received in the DCES Event Database.</li>
     * </ul>
     * @param fdcProcessedRequest Contains the details of the FDC which has been processed by the DRC.
     * @return FileID of the file associated with the fdcId
     */
    public Integer handleFdcProcessedAck(FdcProcessedRequest fdcProcessedRequest) {
        try {
            int result = executeFdcProcessedAckCall(fdcProcessedRequest);
            logFdcAsyncEvent(fdcProcessedRequest, OK);
            return result;
        } catch (WebClientResponseException e){
            logFdcAsyncEvent(fdcProcessedRequest, e.getStatusCode());
            throw e;
        }
    }

    /**
     * Method which will process any Final Defence Cost entries in the correct state for sending to the
     * Debt Recovery Company.
     * <ul>
     * <li>Calls MAAT to prime FDCs eligible for Delayed Pickup, or Fast Tracking for processing.</li>
     * <li>Obtains a full list of all FDCs eligible for processing.</li>
     * <li>Sends each to the DRC</li>
     * <li>Creates a Contributions File for the sent FDCs.</li>
     * <li>Updates each successfully processed FDC to SENT in MAAT.</li>
     * </ul>
     * @return If the process was executed successfully, and the contribution file has been created.
     */
    @Timed(value = "laa_dces_drc_service_process_fdc_daily_files",
            description = "Time taken to process the daily FDC files from DRC and passing this for downstream processing.")
    public boolean processDailyFiles() {
        List<Fdc> fdcList;
        List<Fdc> successfulFdcs = new ArrayList<>();
        Map<BigInteger,String> failedFdcs = new LinkedHashMap<>();
        batchId = eventService.generateBatchId();
        fdcGlobalUpdate();
        fdcList = getFdcList();
        sendFdcListToDrc(fdcList, successfulFdcs, failedFdcs);
        return updateFdcAndCreateFile(successfulFdcs, failedFdcs) != null;
    }

    // Component Methods

    private void fdcGlobalUpdate() {
        FdcGlobalUpdateResponse globalUpdateResponse;
        try {
            globalUpdateResponse = executeFdcGlobalUpdateCall();
        } catch (WebClientResponseException e) {
            // if global update fails, log and continue process as there might be other fdcs available for processing.
            String message = String.format("Failed to complete FDC global update [%s]", e.getMessage());
            logGlobalUpdatePayload(e.getStatusCode(), message);
            return;
        }
        // call is either true, or exception.
        int numUpdates = globalUpdateResponse.getNumberOfUpdates();
        String logMessage = String.format("Updated:%s", numUpdates);
        eventService.logFdc(FDC_GLOBAL_UPDATE, batchId, null, OK, logMessage);
        log.info(logMessage);
    }


    private List<Fdc> getFdcList() throws HttpServerErrorException{
        FdcContributionsResponse response;
        response = executeGetFdcContributionsCall();
        List<Fdc> fdcList = new ArrayList<>();
        if (Objects.nonNull(response)
                && Objects.nonNull(response.getFdcContributions())) {
            // mapped fetched list into our objects.
            List<FdcContributionEntry> fdcContributionEntryList = response.getFdcContributions();
            fdcList = fdcContributionEntryList.stream().map(fdcMapperUtils::mapFdcEntry).toList();
            String successfulPayload = String.format("Fetched:%s",fdcList.size());
            eventService.logFdc(FETCHED_FROM_MAAT, batchId, null, OK, successfulPayload);
            log.info(successfulPayload);
        }
        return fdcList;
    }

    private void sendFdcListToDrc(List<Fdc> fdcList, List<Fdc> successfulFdcs, Map<BigInteger,String> failedFdcs) {
        for (Fdc currentFdc : fdcList) {
            eventService.logFdc(FETCHED_FROM_MAAT, batchId, currentFdc, OK, null);
            int fdcId = currentFdc.getId().intValue();
            try {
                executeSendFdcToDrcCall(currentFdc, fdcId, failedFdcs);
                eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, OK, null);
                successfulFdcs.add(currentFdc);
            } catch (WebClientResponseException e){
                if (FileServiceUtils.isDrcConflict(e)) {
                        log.info("Ignoring duplicate FDC error response from DRC, fdcId = {}, maatId = {}", currentFdc.getId(), currentFdc.getMaatId());
                        eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, CONFLICT, null);
                        successfulFdcs.add(currentFdc);
                        continue;
                    }
                // if not CONFLICT, or not duplicate, then just log it.
                logDrcSentError(e, e.getStatusCode(), currentFdc, failedFdcs);
            }
        }
    }

    private Integer updateFdcAndCreateFile(List<Fdc> successfulFdcs, Map<BigInteger, String> failedFdcs) {
        // If any contributions were sent, then finish off with updates and create the file:
        Integer contributionFileId = null;
        if (!successfulFdcs.isEmpty()) {
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
            contributionFileId = executeFdcFileCreateCall(xmlFile, successfulIdList, successfulIdList.size(), fileName, ackXml);
        }
        logMaatUpdateEvent(successfulFdcs, failedFdcs);
        return contributionFileId;
    }

    // External Call Executions Methods

    private int executeFdcProcessedAckCall(FdcProcessedRequest fdcProcessedRequest) {
        int result;
        if (!feature.incomingIsolated()) {
            result = fdcClient.sendLogFdcProcessed(fdcProcessedRequest);
        } else {
            log.info("Feature:IncomingIsolated: processFdcUpdate: Skipping MAAT API sendLogFdcProcessed() call");
            result = 0; // avoid updating MAAT DB.
        }
        return result;
    }

    @Retry(name = SERVICE_NAME)
    private FdcGlobalUpdateResponse executeFdcGlobalUpdateCall(){
        if (!feature.outgoingIsolated()) {
            return fdcClient.executeFdcGlobalUpdate();
        } else {
            log.info("Feature:OutgoingIsolated: callFdcGlobalUpdate: Skipping MAAT API executeFdcGlobalUpdate() call");
            return new FdcGlobalUpdateResponse(true, 0);
        }
    }

    @Retry(name = SERVICE_NAME)
    private FdcContributionsResponse executeGetFdcContributionsCall() {
        FdcContributionsResponse response;
        try {
            response = fdcClient.getFdcContributions(REQUESTED_STATUS);
        } catch (WebClientResponseException e) {
            // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
            log.error("Failed to retrieve FDC contributions, after the FDC global update completed [" + e.getClass().getSimpleName() + "(" + e.getMessage() + ")]");
            eventService.logFdc(FETCHED_FROM_MAAT, batchId, null, e.getStatusCode(), e.getMessage());
            throw e;
        }
        return response;
    }

    @Retry(name = SERVICE_NAME)
    private void executeSendFdcToDrcCall(Fdc currentFdc, int fdcId, Map<BigInteger,String> failedFdcs) {
        final var request = FdcReqForDrc.of(fdcId, currentFdc);
        if (!feature.outgoingIsolated()) {
            drcClient.sendFdcReqToDrc(request);
            log.info("Sent FDC data to DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
        } else {
            try {
                log.info("Feature:OutgoingIsolated: Skipping FDC data to DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
                final var json = objectMapper.writeValueAsString(request);
                log.debug("Skipping FDC data to DRC, JSON = [{}]", json);
            } catch (JsonProcessingException e) {
                logDrcSentError(e, INTERNAL_SERVER_ERROR, currentFdc, failedFdcs);
            }
        }
    }

    @Retry(name = SERVICE_NAME)
    private Integer executeFdcFileCreateCall(String xmlContent, List<String> fdcIdList, int numberOfRecords, String fileName, String fileAckXML) {
        FdcUpdateRequest request = FdcUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .fdcIds(fdcIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        if (!feature.outgoingIsolated()) {
            try {
                return fdcClient.updateFdcs(request);
            } catch (WebClientResponseException e){
                logFileCreationError(e, e.getStatusCode());
                throw e;
            }
        } else {
            log.info("Feature:OutgoingIsolated: fdcUpdateRequest: Skipping MAAT API updateFdcs() call");
            return 0;
        }
    }

    // Logging Methods

    private void logFdcAsyncEvent(FdcProcessedRequest fdcProcessedRequest, HttpStatusCode httpStatusCode){
        Fdc idHolder = new Fdc();
        idHolder.setId(BigInteger.valueOf(fdcProcessedRequest.getFdcId()));
        eventService.logFdc(DRC_ASYNC_RESPONSE, null, idHolder, httpStatusCode, fdcProcessedRequest.getErrorText());
    }

    private void logGlobalUpdatePayload(HttpStatusCode httpStatus, String message){
        // TODO: Should/How to Flag here for sentry if this is a failure?
        boolean isFailureState = !HttpStatus.ACCEPTED.is2xxSuccessful();
        String payload = (isFailureState ? "Failed to complete FDC global update [%s]" : "%s");
        payload = String.format(payload, message);
        eventService.logFdc(FDC_GLOBAL_UPDATE, batchId, null, httpStatus, payload);
        log.atLevel(isFailureState?Level.ERROR:Level.INFO).log("payload");
    }

    private void logDrcSentError(Exception e, HttpStatusCode httpStatusCode, Fdc currentFdc, Map<BigInteger,String> failedFdcs) {
        // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
        failedFdcs.put(currentFdc.getId(), e.getClass().getSimpleName() + ": " + e.getMessage());
        eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, httpStatusCode, e.getMessage());
    }

    private void logMaatUpdateEvent(List<Fdc> successfulFdcs, Map<BigInteger, String> failedFdcs) {
        // log success and failure numbers.
        eventService.logFdc(UPDATED_IN_MAAT, batchId, null, OK, "Successfully Sent:"+ successfulFdcs.size());
        eventService.logFdc(UPDATED_IN_MAAT, batchId, null, (failedFdcs.size()>0?INTERNAL_SERVER_ERROR:OK), "Failed To Send:"+ failedFdcs.size());
        // insert row for each successfully updated fdc.
        for(Fdc currentFdc: successfulFdcs){
            eventService.logFdc(UPDATED_IN_MAAT, batchId, currentFdc, OK, null);
        }
    }

    private void logFileCreationError(Exception e, HttpStatusCode httpStatusCode) {
        // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
        log.error("Failed to create FDC contribution-file. Investigation needed. State of files will be out of sync! [{}({})]", e.getClass().getSimpleName(), e.getMessage());
        // If failed, we want to handle this. As it will mean the whole process failed for current day.
        eventService.logFdc(UPDATED_IN_MAAT, batchId,null, httpStatusCode, String.format("Failed to create contribution-file: [%s]",e.getMessage()));
    }

}
