package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
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
    private BigInteger batchId;
    @Value("${services.maat-api.getContributionBatchSize:350}")
    private int getContributionBatchSize;

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
    public Integer handleContributionProcessedAck(ContributionProcessedRequest contributionProcessedRequest) {
        try {
            return executeContributionProcessedAckCall(contributionProcessedRequest);
        } catch (WebClientResponseException e) {
            logContributionAsyncEvent(contributionProcessedRequest, e.getStatusCode());
            throw FileServiceUtils.translateMAATCDAPIException(e);
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
    @Timed(value = "laa_dces_drc_service_process_contributions_daily_files",
            description = "Time taken to process the daily contributions files from DRC and passing this for downstream processing.")
    public boolean processDailyFiles() {
        batchId = eventService.generateBatchId();
        List<ConcorContribEntry> contributionsList;
        List<Integer> receivedContributionFileIds = new ArrayList<>();
        int startingId = 0;
        do {
            Map<BigInteger, CONTRIBUTIONS> successfulContributions = new LinkedHashMap<>();
            Map<BigInteger, String> failedContributions = new LinkedHashMap<>();
            contributionsList = getContributionList(startingId);
            if (!contributionsList.isEmpty()) {
                sendContributionListToDrc(contributionsList, successfulContributions, failedContributions);
                Integer contributionFileId = updateContributionsAndCreateFile(successfulContributions, failedContributions);

                receivedContributionFileIds.add(contributionFileId);
                log.info("Created contribution-file ID {}", contributionFileId);
                startingId = contributionsList.get(contributionsList.size() - 1).getConcorContributionId();
            }
        } while (contributionsList.size() == getContributionBatchSize);

        return !receivedContributionFileIds.isEmpty() && !receivedContributionFileIds.contains(null);
    }

    // Component Methods

    // Should this be kept? This is more for readability/formatting. Feels wrong.
    private List<ConcorContribEntry> getContributionList(int startingId) {
        return executeGetContributionsCall(startingId);
    }

    private void sendContributionListToDrc(List<ConcorContribEntry> contributionsList, Map<BigInteger, CONTRIBUTIONS> successfulContributions, Map<BigInteger, String> failedContributions) {
        // for each contribution sent by MAAT API
        for (ConcorContribEntry contribEntry : contributionsList) {
            final BigInteger concorContributionId = BigInteger.valueOf(contribEntry.getConcorContributionId());
            CONTRIBUTIONS currentContribution = mapContributionXmlToObject(concorContributionId, contribEntry.getXmlContent(), failedContributions);
            if (Objects.nonNull(currentContribution)) {
                try {
                    executeSendConcorToDrcCall(concorContributionId, currentContribution, failedContributions);
                    eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, OK, null);
                    successfulContributions.put(concorContributionId, currentContribution);
                } catch (WebClientResponseException e) {
                    if (FileServiceUtils.isDrcConflict(e)) {
                        log.info("Ignoring duplicate contribution error response from DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
                        eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, CONFLICT, null);
                        successfulContributions.put(concorContributionId, currentContribution);
                        continue;
                    }
                    logDrcSendError(e, e.getStatusCode(), concorContributionId, currentContribution, failedContributions);
                }
            }
        }
    }

    private Integer updateContributionsAndCreateFile(Map<BigInteger, CONTRIBUTIONS> successfulContributions, Map<BigInteger, String> failedContributions) {
        // If any contributions were sent, then create XML file:
        Integer contributionFileId = null;
        if (!successfulContributions.isEmpty()) {
            // Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            LocalDateTime dateGenerated = LocalDateTime.now();
            String fileName = contributionsMapperUtils.generateFileName(dateGenerated);
            String xmlFile = contributionsMapperUtils.generateFileXML(successfulContributions.values().stream().toList(), fileName);
            String ackXml = contributionsMapperUtils.generateAckXML(fileName, dateGenerated.toLocalDate(), failedContributions.size(), successfulContributions.size());
            List<BigInteger> successfulIdList = successfulContributions.keySet().stream().toList();
            contributionFileId = executeConcorFileCreateCall(xmlFile, successfulIdList, successfulIdList.size(), fileName, ackXml);
        }
        logMaatUpdateEvents(successfulContributions, failedContributions);
        return contributionFileId;
    }


    private CONTRIBUTIONS mapContributionXmlToObject(BigInteger concorContributionId, String xmlEntry, Map<BigInteger, String> failedContributions) {
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
    private int executeContributionProcessedAckCall(ContributionProcessedRequest contributionProcessedRequest) {
        int result;
        if (!feature.incomingIsolated()) {
            result = contributionClient.sendLogContributionProcessed(contributionProcessedRequest);
        } else {
            log.info("Feature:IncomingIsolated: processContributionUpdate: Skipping MAAT API sendLogContributionProcessed() call");
            result = 0; // avoid updating MAAT DB.
        }
        logContributionAsyncEvent(contributionProcessedRequest, OK);
        return result;
    }

    @Retry(name = SERVICE_NAME)
    private List<ConcorContribEntry> executeGetContributionsCall(int startingId) {
        List<ConcorContribEntry> contributionsList = contributionClient.getContributions(ContributionRecordStatus.ACTIVE.name(), startingId, getContributionBatchSize);
        eventService.logConcor(null, FETCHED_FROM_MAAT, batchId, null, OK, String.format("Fetched:%s",contributionsList.size()));
        return contributionsList;
    }

    @Retry(name = SERVICE_NAME)
    private void executeSendConcorToDrcCall(BigInteger concorContributionId, CONTRIBUTIONS currentContribution, Map<BigInteger, String> failedContributions) {
        final var request = ConcorContributionReqForDrc.of(concorContributionId.intValue(), currentContribution);
        if (!feature.outgoingIsolated()) {
            drcClient.sendConcorContributionReqToDrc(request);
            log.info("Sent contribution data to DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
        } else {
            log.info("Feature:OutgoingIsolated: Skipping contribution data to DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
            try {
                final var json = objectMapper.writeValueAsString(request);
                log.debug("Skipping contribution data to DRC, JSON = [{}]", json);
            } catch (JsonProcessingException e) {
                logDrcSendError(e, INTERNAL_SERVER_ERROR, concorContributionId, currentContribution, failedContributions);
            }
        }
    }

    @Retry(name = SERVICE_NAME)
    private Integer executeConcorFileCreateCall(String xmlContent, List<BigInteger> concorContributionIdList, int numberOfRecords, String fileName, String fileAckXML) {
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
                logFileCreationError(e, e.getStatusCode());
                throw e;
            }
        } else {
            log.info("Feature:OutgoingIsolated: contributionUpdateRequest: Skipping MAAT API updateContributions() call");
            return 0;
        }
    }

    // Logging Methods


    private void logContributionAsyncEvent(ContributionProcessedRequest contributionProcessedRequest, HttpStatusCode httpStatusCode){
        BigInteger concorId = BigInteger.valueOf(contributionProcessedRequest.getConcorId());
        eventService.logConcor(concorId, DRC_ASYNC_RESPONSE, null, null, httpStatusCode, contributionProcessedRequest.getErrorText());
    }

    private void logDrcSendError(Exception e, HttpStatusCode httpStatusCode, BigInteger concorContributionId, CONTRIBUTIONS currentContribution, Map<BigInteger,String> failedContributions) {
        // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
        failedContributions.put(concorContributionId, e.getClass().getSimpleName() + ": " + e.getMessage());
        eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, httpStatusCode, e.getMessage());
    }

    private void logMaatUpdateEvents(Map<BigInteger, CONTRIBUTIONS> successfulContributions, Map<BigInteger, String> failedContributions) {
        // log success and failure numbers.
        eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, OK, "Successfully Sent:"+ successfulContributions.size());
        eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, (failedContributions.size()>0?INTERNAL_SERVER_ERROR:OK), "Failed To Send:"+ failedContributions.size());
        // Explicitly log the Concor contribution IDs that were updated:
        for(Map.Entry<BigInteger, CONTRIBUTIONS> currentContribution: successfulContributions.entrySet()){
            eventService.logConcor(currentContribution.getKey(),UPDATED_IN_MAAT, batchId, currentContribution.getValue(), OK, null);
        }
    }

    private void logFileCreationError(WebClientResponseException e, HttpStatusCode httpStatusCode) {
        log.error("Failed to create Concor contribution-file. Investigation needed. State of files will be out of sync! [{}({})]", e.getClass().getSimpleName(), e.getMessage());
        eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, httpStatusCode, String.format("Failed to create contribution-file: [%s]",e.getMessage()));
    }

}
