package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.enums.ContributionRecordStatus;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;
import uk.gov.justice.laa.crime.dces.integration.utils.FileServiceUtils;
import uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils;

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
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.FETCHED_FROM_MAAT;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.SENT_TO_DRC;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.UPDATED_IN_MAAT;

@RequiredArgsConstructor
@Service
@Slf4j
public class ContributionService implements FileService {
    private static final String SERVICE_NAME = "ContributionService";
    private final ContributionsMapperUtils contributionsMapperUtils;
    private final ContributionClient contributionClient;
    private final DrcClient drcClient;
    private final ObjectMapper objectMapper;
    private final FeatureProperties feature;
    private final AnonymisingDataService anonymisingDataService;
    private final EventService eventService;
    private Long batchId;
    @Value("${services.maat-api.getContributionBatchSize:350}")
    private int getContributionBatchSize;
    private final MeterRegistry meterRegistry;

    /**
     * Method which logs that a specific contribution has been processed by the Debt Recovery Company.
     * <ul>
     * <li>Will log a success by incrementing the successful count of the associated contribution file.</li>
     * <li>If error text is present, will instead log it to the MAAT DB as an error for the associated contribution file.</li>
     * <li>Logs details received in the DCES Event Database.</li>
     * </ul>
     * @param contributionProcessedRequest Contains the details of the concor contribution which has been processed by the DRC.
     * @return FileID of the file associated with the fdcId
     */
    public Long handleContributionProcessedAck(ContributionProcessedRequest contributionProcessedRequest) {

        Timer.Sample timerSample = Timer.start(meterRegistry);
        try {
            return executeContributionProcessedAckCall(contributionProcessedRequest);
        } catch (WebClientResponseException e) {
            logContributionAsyncEvent(contributionProcessedRequest, e.getStatusCode());
            throw FileServiceUtils.translateMAATCDAPIException(e);
        } finally {
            timerSample.stop(getTimer(SERVICE_NAME,
                    "method", "handleContributionProcessedAck",
                    "description", "Processing Updates From External for Contribution"));
        }
    }

    /**
     * Method which will process any Concor Contribution entries in the correct state for sending to the
     * Debt Recovery Company.
     * <ul>
     * <li>Obtains a full list of all concor contributions eligible for processing.</li>
     * <li>Sends each to the DRC</li>
     * <li>Creates a Contributions File for the sent concor contributions.</li>
     * <li>Updates each successfully processed concor contributions to SENT in MAAT.</li>
     * </ul>
     * @return If the process was executed successfully, and the contribution file has been created.
     */
    public boolean processDailyFiles() {

        Timer.Sample timerSample = Timer.start(meterRegistry);

        batchId = eventService.generateBatchId();
        List<ConcorContribEntry> contributionsList;
        List<Long> receivedContributionFileIds = new ArrayList<>();
        Long startingId = 0L;
        do {
            Map<Long, CONTRIBUTIONS> successfulContributions = new LinkedHashMap<>();
            Map<Long, String> failedContributions = new LinkedHashMap<>();
            contributionsList = executeGetContributionsCall(startingId);
            if (!contributionsList.isEmpty()) {
                sendContributionListToDrc(contributionsList, successfulContributions, failedContributions);
                Long contributionFileId = updateContributionsAndCreateFile(successfulContributions, failedContributions);

                receivedContributionFileIds.add(contributionFileId);
                log.info("Created contribution-file ID {}", contributionFileId);
                startingId = contributionsList.get(contributionsList.size() - 1).getConcorContributionId();
            }
        } while (contributionsList.size() == getContributionBatchSize);

        timerSample.stop(getTimer(SERVICE_NAME,
                "method", "processDailyFiles",
                "description", "Time taken to process the daily contributions files from DRC and passing this for downstream processing."
        ));

        return !receivedContributionFileIds.isEmpty() && !receivedContributionFileIds.contains(null);
    }

    /**
     * Method to be used during testing which will get the XMLs for the concor Contribution IDs provided and send them to the
     * Debt Recovery Company.
     * <ul>
     * <li>Obtains XML for all concor contribution IDs in the list.</li>
     * <li>Sends each to the DRC</li>
     * <li>Does NOT create a Contributions File for the sent concor contributions.</li>
     * <li>Does NOT update each successfully processed concor contribution to SENT in MAAT.</li>
     * </ul>
     * @return List of Concor Contribution Entries (containing the ID and XML) that were sent to DRC.
     */
    @Timed(value = "laa_dces_drc_service_send_contributions_to_DRC",
        description = "Time taken to get contributions XML from MAAT API and passing this to DRC.")
    public List<ConcorContribEntry> sendContributionsToDrc(List<Long> idList) {
        List<ConcorContribEntry> contributionsList;
        Map<Long, CONTRIBUTIONS> successfulContributions = new LinkedHashMap<>();
        Map<Long, String> failedContributions = new LinkedHashMap<>();
        contributionsList = executeGetContributionsCall(idList);
        if (!contributionsList.isEmpty()) {
            sendContributionListToDrc(contributionsList, successfulContributions, failedContributions);
            log.info("Sent {} concor contributions to the DRC, {} successful, {} failed", contributionsList.size(), successfulContributions.size(), failedContributions.size());
        }

        return contributionsList;
    }

    // Component Methods

    private void sendContributionListToDrc(List<ConcorContribEntry> contributionsList, Map<Long, CONTRIBUTIONS> successfulContributions, Map<Long, String> failedContributions) {
        // for each contribution sent by MAAT API
        for (ConcorContribEntry contribEntry : contributionsList) {
            final Long concorContributionId = contribEntry.getConcorContributionId();
            CONTRIBUTIONS currentContribution = mapContributionXmlToObject(concorContributionId, contribEntry.getXmlContent(), failedContributions);
            if (Objects.nonNull(currentContribution)) {
                try {
                    String response = executeSendConcorToDrcCall(concorContributionId, currentContribution, failedContributions);
                    int pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus(response);
                    if (MapperUtils.successfulStatus(pseudoStatusCode)) {
                        eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, HttpStatusCode.valueOf(pseudoStatusCode), response);
                        successfulContributions.put(concorContributionId, currentContribution);
                    } else {
                        // if we didn't get a valid response, record an error status code 635, and try again next time.
                        failedContributions.put(concorContributionId, "Invalid JSON response body from DRC");
                        eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, HttpStatusCode.valueOf(pseudoStatusCode), response);
                    }
                } catch (WebClientResponseException e) {
                    if (FileServiceUtils.isDrcConflict(e)) {
                        log.info("Ignoring duplicate contribution error response from DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
                        eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, CONFLICT, e.getResponseBodyAsString());
                        successfulContributions.put(concorContributionId, currentContribution);
                    } else {
                        // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                        failedContributions.put(concorContributionId, e.getClass().getSimpleName() + ": " + e.getResponseBodyAsString());
                        eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, e.getStatusCode(), e.getResponseBodyAsString());
                    }
                }
            }
        }
    }

    private Long updateContributionsAndCreateFile(Map<Long, CONTRIBUTIONS> successfulContributions, Map<Long, String> failedContributions) {
        // If any contributions were sent, then create XML file:
        Long contributionFileId = null;
        if (!successfulContributions.isEmpty()) {
            // Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            LocalDateTime dateGenerated = LocalDateTime.now();
            String fileName = contributionsMapperUtils.generateFileName(dateGenerated);
            String xmlFile = contributionsMapperUtils.generateFileXML(successfulContributions.values().stream().toList(), fileName);
            String ackXml = contributionsMapperUtils.generateAckXML(fileName, dateGenerated.toLocalDate(), failedContributions.size(), successfulContributions.size());
            List<Long> successfulIdList = successfulContributions.keySet().stream().toList();
            contributionFileId = executeConcorFileCreateCall(xmlFile, successfulIdList, successfulIdList.size(), fileName, ackXml);
        }
        logMaatUpdateEvents(successfulContributions, failedContributions);
        return contributionFileId;
    }


    private CONTRIBUTIONS mapContributionXmlToObject(Long concorContributionId, String xmlEntry, Map<Long, String> failedContributions) {
        CONTRIBUTIONS currentContribution;
        try {
            currentContribution = contributionsMapperUtils.mapLineXMLToObject(xmlEntry);
        } catch (JAXBException e) {
            failedContributions.put(concorContributionId, e.getClass().getName() + ": " + e.getMessage());
            log.error("Failed to unmarshal contribution data XML, concorContributionId = {}", concorContributionId, e);
            eventService.logConcor(concorContributionId, FETCHED_FROM_MAAT, batchId, null, INTERNAL_SERVER_ERROR, "Failed to unmarshal contribution data XML");
            return null;
        }

        if (feature.outgoingAnonymized()) {
            log.info("Feature:OutgoingAnonymized: contribution data will be anonymized.");
            currentContribution = anonymisingDataService.anonymise(currentContribution);
        }
        eventService.logConcor(concorContributionId, FETCHED_FROM_MAAT, batchId, currentContribution, OK, null);
        return currentContribution;
    }

    // External Call Executions Methods

    @Retry(name = SERVICE_NAME)
    public long executeContributionProcessedAckCall(ContributionProcessedRequest contributionProcessedRequest) {
        long result = 0L;
        if (!feature.incomingIsolated()) {
            result = contributionClient.sendLogContributionProcessed(contributionProcessedRequest);
        } else {
            log.info("Feature:IncomingIsolated: processContributionUpdate: Skipping MAAT API sendLogContributionProcessed() call");
        }
        logContributionAsyncEvent(contributionProcessedRequest, OK);
        return result;
    }

    @Retry(name = SERVICE_NAME)
    public List<ConcorContribEntry> executeGetContributionsCall(Long startingId) {
        List<ConcorContribEntry> contributionsList = contributionClient.getContributions(ContributionRecordStatus.ACTIVE.name(), startingId, getContributionBatchSize);
        eventService.logConcor(null, FETCHED_FROM_MAAT, batchId, null, OK, String.format("Fetched:%s",contributionsList.size()));
        return contributionsList;
    }

    @Retry(name = SERVICE_NAME)
    private List<ConcorContribEntry> executeGetContributionsCall(List<Long> idList) {
        List<ConcorContribEntry> contributionsList = contributionClient.getConcorListById(idList);
        eventService.logConcor(null, FETCHED_FROM_MAAT, batchId, null, OK, String.format("Fetched:%s",contributionsList.size()));
        return contributionsList;
    }

    @Retry(name = SERVICE_NAME)
    private String executeSendConcorToDrcCall(Long concorContributionId, CONTRIBUTIONS currentContribution, Map<Long, String> failedContributions) {
        final var request = ConcorContributionReqForDrc.of(concorContributionId, currentContribution);
        String responsePayload = null;
        if (!feature.outgoingIsolated()) {
            responsePayload = drcClient.sendConcorContributionReqToDrc(request);
            log.info("Sent contribution data to DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
        } else {
            log.info("Feature:OutgoingIsolated: Skipping contribution data to DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
            try {
                final var json = objectMapper.writeValueAsString(request);
                log.debug("Skipping contribution data to DRC, JSON = [{}]", json);
                responsePayload = "{\"meta\":{\"drcId\":1,\"concorContributionId\":"
                        + concorContributionId + ",\"skippedDueToFeatureOutgoingIsolated\":true}}";
            } catch (JsonProcessingException e) {
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedContributions.put(concorContributionId, e.getClass().getSimpleName() + ": " + e.getMessage());
                eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
        return responsePayload;
    }

    @Retry(name = SERVICE_NAME)
    public Long executeConcorFileCreateCall(String xmlContent, List<Long> concorContributionIdList, int numberOfRecords, String fileName, String fileAckXML) {
        log.info("Sending contribution update request to MAAT API for {}", concorContributionIdList);

        ContributionUpdateRequest request = ContributionUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .concorContributionIds(concorContributionIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        if (!feature.outgoingIsolated()) {
            try {
                return contributionClient.updateContributions(request);
            } catch (WebClientResponseException e){
                logFileCreationError(e);
                throw e;
            }
        } else {
            log.info("Feature:OutgoingIsolated: contributionUpdateRequest: Skipping MAAT API updateContributions() call");
            return 0L;
        }
    }

    // Logging Methods


    private void logContributionAsyncEvent(ContributionProcessedRequest contributionProcessedRequest, HttpStatusCode httpStatusCode){
        eventService.logConcor(contributionProcessedRequest.getConcorId(), DRC_ASYNC_RESPONSE, null, null, httpStatusCode, contributionProcessedRequest.getErrorText());
    }

    private void logMaatUpdateEvents(Map<Long, CONTRIBUTIONS> successfulContributions, Map<Long, String> failedContributions) {
        // log success and failure numbers.
        eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, OK, "Successfully Sent:"+ successfulContributions.size());
        eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, (!failedContributions.isEmpty()
            ?INTERNAL_SERVER_ERROR:OK), "Failed To Send:"+ failedContributions.size());
        // Explicitly log the Concor contribution IDs that were updated:
        for(Map.Entry<Long, CONTRIBUTIONS> currentContribution: successfulContributions.entrySet()){
            eventService.logConcor(currentContribution.getKey(),UPDATED_IN_MAAT, batchId, currentContribution.getValue(), OK, null);
        }
    }

    private void logFileCreationError(WebClientResponseException e) {
        log.error("Failed to create Concor contribution-file. Investigation needed. State of files will be out of sync! [{}:({})]", e.getClass().getSimpleName(), e.getResponseBodyAsString());
        eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, e.getStatusCode(), String.format("Failed to create contribution-file: Message:[%s] | Response:[%s]",e.getMessage(), e.getResponseBodyAsString()));
    }

    private Timer getTimer(String name, String... tagsMap) {
        return Timer.builder(name)
                .tags(tagsMap)
                .register(meterRegistry);
    }
}
