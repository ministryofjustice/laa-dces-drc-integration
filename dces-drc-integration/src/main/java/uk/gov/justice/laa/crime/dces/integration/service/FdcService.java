package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public static final String REQUESTED_STATUS = "REQUESTED";
    private static final String SERVICE_NAME = "FdcService";
    private final FdcMapperUtils fdcMapperUtils;
    private final FdcClient fdcClient;
    private final DrcClient drcClient;
    private final ObjectMapper objectMapper;
    private final FeatureProperties feature;
    private final EventService eventService;
    private final MeterRegistry meterRegistry;
    private Long batchId;

    /**
     * Method which logs that a specific fdc has been processed by the Debt Recovery Company.
     * <ul>
     * <li>Will log a success by incrementing the successful count of the associated contribution file.</li>
     * <li>If error text is present, will instead log it to the MAAT DB as an error for the associated contribution file.</li>
     * <li>Logs details received in the DCES Event Database.</li>
     * </ul>
     *
     * @param fdcProcessedRequest Contains the details of the FDC which has been processed by the DRC.
     * @return FileID of the file associated with the fdcId
     */
    public long handleFdcProcessedAck(FdcProcessedRequest fdcProcessedRequest) {
        Timer.Sample timerSample = Timer.start(meterRegistry);
        try {
            long result = executeFdcProcessedAckCall(fdcProcessedRequest);
            logFdcAsyncEvent(fdcProcessedRequest, OK);
            return result;
        } catch (WebClientResponseException e) {
            logFdcAsyncEvent(fdcProcessedRequest, e.getStatusCode());
            throw FileServiceUtils.translateMAATCDAPIException(e);
        } finally {
            timerSample.stop(getTimer(SERVICE_NAME,
                    "method", "handleFdcProcessedAck",
                    "description", "Time taken to process the acknowledgement for the FDC updates."));
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
     *
     * @return If the process was executed successfully, and the contribution file has been created.
     */
    public boolean processDailyFiles() {

        Timer.Sample timerSample = Timer.start(meterRegistry);

        List<Fdc> fdcList;
        List<Fdc> successfulFdcs = new ArrayList<>();
        Map<Long, String> failedFdcs = new LinkedHashMap<>();

        try {
            batchId = eventService.generateBatchId();
            fdcGlobalUpdate();
            fdcList = getFdcList();
            sendFdcListToDrc(fdcList, successfulFdcs, failedFdcs);
            return updateFdcAndCreateFile(successfulFdcs, failedFdcs) != null;
        } finally {
            timerSample.stop(getTimer(SERVICE_NAME,
                    "method", "processDailyFiles",
                    "description", "Time taken to process the daily FDC files from DRC and passing this for downstream processing.",
                    "successfulFiles", String.valueOf(successfulFdcs.size()),
                    "failedFiles", String.valueOf(failedFdcs.size())));
        }
    }

    /**
     * Method to be used during testing which will get the data for the FDC IDs provided and send them to the
     * Debt Recovery Company.
     * <ul>
     * <li>Obtains data for all FDC IDs in the list.</li>
     * <li>Sends each to the DRC</li>
     * <li>Does NOT create a Contributions File for the sent FDC contributions.</li>
     * <li>Does NOT update each successfully processed FDC contribution to SENT in MAAT.</li>
     * </ul>
     * @return @return List of FDC Entries (containing the ID and XML) that were sent to DRC
     */
    @Timed(value = "laa_dces_drc_service_process_contributions_daily_files",
        description = "Time taken to process the daily contributions files from DRC and passing this for downstream processing.")
    public List<Fdc> sendFdcsToDrc(List<Long> idList) {
        List<Fdc> fdcList;
        List<Fdc> successfulFdcs = new ArrayList<>();
        Map<Long,String> failedFdcs = new LinkedHashMap<>();
        FdcContributionsResponse response = executeGetFdcContributionsCall(idList);
        fdcList = response.getFdcContributions().stream().map(fdcMapperUtils::mapFdcEntry).toList();
        if (!fdcList.isEmpty()) {
            sendFdcListToDrc(fdcList, successfulFdcs, failedFdcs);
            log.info("Sent {} concor contributions to the DRC, {} successful, {} failed", fdcList.size(), successfulFdcs.size(), failedFdcs.size());
        }
        return fdcList;
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
            String successfulPayload = String.format("Fetched:%s", fdcList.size());
            eventService.logFdc(FETCHED_FROM_MAAT, batchId, null, OK, successfulPayload);
            log.info(successfulPayload);
        }
        return fdcList;
    }

    private void sendFdcListToDrc(List<Fdc> fdcList, List<Fdc> successfulFdcs, Map<Long, String> failedFdcs) {
        for (Fdc currentFdc : fdcList) {
            eventService.logFdc(FETCHED_FROM_MAAT, batchId, currentFdc, OK, null);
            int fdcId = currentFdc.getId().intValue();
            try {
                String responsePayload = executeSendFdcToDrcCall(currentFdc, fdcId, failedFdcs);
                HttpStatusCode pseudoStatusCode = FdcMapperUtils.mapDRCJsonResponseToHttpStatus(responsePayload);
                if (MapperUtils.successfulStatus(pseudoStatusCode)) {
                    eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, pseudoStatusCode, responsePayload);
                    successfulFdcs.add(currentFdc);
                } else {
                    // if we didn't get a valid response, record an error status code 635, and try again next time.
                    failedFdcs.put(currentFdc.getId(), "Invalid JSON response body from DRC");
                    eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, pseudoStatusCode, responsePayload);
                }
            } catch (WebClientResponseException e){
                if (FileServiceUtils.isDrcConflict(e)) {
                    log.info("Ignoring duplicate FDC error response from DRC, fdcId = {}, maatId = {}", currentFdc.getId(), currentFdc.getMaatId());
                    eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, MapperUtils.STATUS_CONFLICT_DUPLICATE_ID, e.getResponseBodyAsString());
                    successfulFdcs.add(currentFdc);
                } else {
                    // if not CONFLICT, or not duplicate, then just log it.
                    logDrcSentError(e, e.getStatusCode(), currentFdc, failedFdcs);
                }
            }
        }
    }

    private Long updateFdcAndCreateFile(List<Fdc> successfulFdcs, Map<Long, String> failedFdcs) {
        // If any contributions were sent, then finish off with updates and create the file:
        Long contributionFileId = null;
        if (!successfulFdcs.isEmpty()) {
            // Construct other parameters for the "ATOMIC UPDATE" call.
            LocalDateTime dateGenerated = LocalDateTime.now();
            String fileName = fdcMapperUtils.generateFileName(dateGenerated);
            String ackXml = fdcMapperUtils.generateAckXML(fileName, dateGenerated.toLocalDate(), failedFdcs.size(), successfulFdcs.size());
            String xmlFile = fdcMapperUtils.generateFileXML(successfulFdcs, fileName);
            List<String> successfulIdList = successfulFdcs.stream()
                    .map(Fdc::getId)
                    .filter(Objects::nonNull)
                    .map(Object::toString).toList();

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
    @Retry(name = SERVICE_NAME)
    public long executeFdcProcessedAckCall(FdcProcessedRequest fdcProcessedRequest) {
        long result = 0L;
        if (!feature.incomingIsolated()) {
            result = fdcClient.sendLogFdcProcessed(fdcProcessedRequest);
        } else {
            log.info("Feature:IncomingIsolated: processFdcUpdate: Skipping MAAT API sendLogFdcProcessed() call");
        }
        return result;
    }

    @Retry(name = SERVICE_NAME)
    public FdcGlobalUpdateResponse executeFdcGlobalUpdateCall(){
        if (!feature.outgoingIsolated()) {
            return fdcClient.executeFdcGlobalUpdate();
        } else {
            log.info("Feature:OutgoingIsolated: callFdcGlobalUpdate: Skipping MAAT API executeFdcGlobalUpdate() call");
            return new FdcGlobalUpdateResponse(true, 0);
        }
    }

    @Retry(name = SERVICE_NAME)
    public FdcContributionsResponse executeGetFdcContributionsCall() {
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
    private FdcContributionsResponse executeGetFdcContributionsCall(List<Long> idList) {
        FdcContributionsResponse response = fdcClient.getFdcListById(idList);
        eventService.logFdc(FETCHED_FROM_MAAT, batchId, null, OK, String.format("Fetched:%s",response.getFdcContributions().size()));
        return response;
    }

    @Retry(name = SERVICE_NAME)
    private String executeSendFdcToDrcCall(Fdc currentFdc, int fdcId, Map<Long,String> failedFdcs) {
        final var request = FdcReqForDrc.of(fdcId, currentFdc);
        String response =null;
        if (!feature.outgoingIsolated()) {
            response = drcClient.sendFdcReqToDrc(request);
            log.info("Sent FDC data to DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
        } else {
            try {
                log.info("Feature:OutgoingIsolated: Skipping FDC data to DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
                final var json = objectMapper.writeValueAsString(request);
                log.debug("Skipping FDC data to DRC, JSON = [{}]", json);
                response = "{\"meta\":{\"drcId\":1,\"fdcId\":"
                        + fdcId + ",\"skippedDueToFeatureOutgoingIsolated\":true}}";
            } catch (JsonProcessingException e) {
                logDrcSentError(e, INTERNAL_SERVER_ERROR, currentFdc, failedFdcs);
            }
        }
        return response;
    }

    @Retry(name = SERVICE_NAME)
    public Long executeFdcFileCreateCall(String xmlContent, List<String> fdcIdList, int numberOfRecords, String fileName, String fileAckXML) {
        FdcUpdateRequest request = FdcUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .fdcIds(fdcIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        if (!feature.outgoingIsolated()) {
            try {
                return fdcClient.updateFdcs(request);
            } catch (WebClientResponseException e) {
                logFileCreationError(e);
                throw e;
            }
        } else {
            log.info("Feature:OutgoingIsolated: fdcUpdateRequest: Skipping MAAT API updateFdcs() call");
            return 0L;
        }
    }

    // Logging Methods

    private void logFdcAsyncEvent(FdcProcessedRequest fdcProcessedRequest, HttpStatusCode httpStatusCode) {
        Fdc idHolder = new Fdc();
        idHolder.setId(fdcProcessedRequest.getFdcId());
        eventService.logFdc(DRC_ASYNC_RESPONSE, null, idHolder, httpStatusCode, fdcProcessedRequest.getErrorText());
    }

    private void logGlobalUpdatePayload(HttpStatusCode httpStatus, String message) {
        boolean isFailureState = !HttpStatus.ACCEPTED.is2xxSuccessful();
        String payload = (isFailureState ? "Failed to complete FDC global update [%s]" : "%s");
        payload = String.format(payload, message);
        eventService.logFdc(FDC_GLOBAL_UPDATE, batchId, null, httpStatus, payload);
        log.atLevel(isFailureState ? Level.ERROR : Level.INFO).log(payload);
    }

    private void logDrcSentError(Exception e, HttpStatusCode httpStatusCode, Fdc currentFdc, Map<Long, String> failedFdcs) {
        // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
        failedFdcs.put(currentFdc.getId(), e.getClass().getSimpleName() + ": " + e.getMessage());
        eventService.logFdc(SENT_TO_DRC, batchId, currentFdc, httpStatusCode, e.getMessage());
    }

    private void logMaatUpdateEvent(List<Fdc> successfulFdcs, Map<Long, String> failedFdcs) {
        // log success and failure numbers.
        eventService.logFdc(UPDATED_IN_MAAT, batchId, null, OK, "Successfully Sent:"+ successfulFdcs.size());
        eventService.logFdc(UPDATED_IN_MAAT, batchId, null, (!failedFdcs.isEmpty() ?INTERNAL_SERVER_ERROR:OK), "Failed To Send:"+ failedFdcs.size());
        // insert row for each successfully updated fdc.
        for (Fdc currentFdc : successfulFdcs) {
            eventService.logFdc(UPDATED_IN_MAAT, batchId, currentFdc, OK, null);
        }
    }

    private void logFileCreationError(WebClientResponseException e) {
        // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
        log.error("Failed to create FDC contribution-file. Investigation needed. State of files will be out of sync! [{}({})]", e.getClass().getSimpleName(), e.getResponseBodyAsString());
        eventService.logFdc(UPDATED_IN_MAAT, batchId, null, e.getStatusCode(), String.format("Failed to create contribution-file: Message: [%s] | Response: [%s]", e.getMessage(), e.getResponseBodyAsString()));
    }

    private Timer getTimer(String name, String... tagsMap) {
        return Timer.builder(name)
                .tags(tagsMap)
                .register(meterRegistry);
    }
}
