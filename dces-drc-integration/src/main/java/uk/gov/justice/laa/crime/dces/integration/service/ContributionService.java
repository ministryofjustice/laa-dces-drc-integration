package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.Feature;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class ContributionService implements FileService {
    private static final String SERVICE_NAME = "ContributionService";
    private final ContributionsMapperUtils contributionsMapperUtils;
    private final ContributionClient contributionClient;
    private final DrcClient drcClient;
    private final ObjectMapper objectMapper;
    private final Feature feature;
    private final AnonymisingDataService anonymisingDataService;
    private final EventService eventService;
    private BigInteger batchId;

    @SuppressWarnings("squid:S2147")  // Duplicate code is catch blocks. However they cannot be merged, due to lacking
    // a shared superclass with .getStatusCode() Same with the throws, to avoid the compiler complaining about throwing
    // a generic "Exception" back up the chain.
    public Integer processContributionUpdate(UpdateLogContributionRequest updateLogContributionRequest) {
        try {
            int result;
            if (!feature.incomingIsolated()) {
                result = contributionClient.sendLogContributionProcessed(updateLogContributionRequest);
            } else {
                result = 0; // avoid updating MAAT DB.
            }
            logContributionAsyncEvent(updateLogContributionRequest, OK);
            return result;
        } catch (WebClientResponseException e){
            logContributionAsyncEvent(updateLogContributionRequest, e.getStatusCode());
            throw e;
        } catch (HttpServerErrorException e ){
            logContributionAsyncEvent(updateLogContributionRequest, e.getStatusCode());
            throw e;
        } catch (MaatApiClientException  e) {
            logContributionAsyncEvent(updateLogContributionRequest, e.getStatusCode());
            throw e;
        }
    }

    private void logContributionAsyncEvent(UpdateLogContributionRequest updateLogContributionRequest, HttpStatusCode httpStatusCode){
        BigInteger concorId = BigInteger.valueOf(updateLogContributionRequest.getConcorId());
        eventService.logConcor(concorId, DRC_ASYNC_RESPONSE, null, null, httpStatusCode, updateLogContributionRequest.getErrorText());
    }

    @Timed(value = "laa_dces_drc_service_process_contributions_daily_files",
            description = "Time taken to process the daily contributions files from DRC and passing this for downstream processing.")
    public boolean processDailyFiles() {
        batchId = eventService.generateBatchId();

        Map<String, CONTRIBUTIONS> successfulContributions = new HashMap<>();
        Map<String, String> failedContributions = new HashMap<>();

        List<ConcurContribEntry> contributionsList = contributionClient.getContributions("ACTIVE");
        String successfulPayload = "Fetched "+contributionsList.size()+" concorContribution entries";
        eventService.logConcor(null, FETCHED_FROM_MAAT, batchId, null, OK, successfulPayload);

        sendContributionsToDrc(contributionsList, successfulContributions, failedContributions);
        return updateContributionsAndCreateFile(successfulContributions, failedContributions) != null;
    }


    @Retry(name = SERVICE_NAME)
    public void sendContributionsToDrc(List<ConcurContribEntry> contributionsList, Map<String, CONTRIBUTIONS> successfulContributions, Map<String,String> failedContributions){
        // for each contribution sent by MAAT API
        for (ConcurContribEntry contribEntry : contributionsList) {
            final BigInteger concorContributionId = BigInteger.valueOf(contribEntry.getConcorContributionId());
            // convert string into objects
            CONTRIBUTIONS currentContribution;
            try {
                currentContribution = contributionsMapperUtils.mapLineXMLToObject(contribEntry.getXmlContent());
                if (feature.outgoingAnonymized()) {
                    // anonymize the data when flag is true - only for non production environments
                    currentContribution = anonymisingDataService.anonymise(currentContribution);
                }
                eventService.logConcor(concorContributionId, FETCHED_FROM_MAAT, batchId, currentContribution, OK, null);
            } catch (JAXBException e) {
                failedContributions.put(concorContributionId.toString(), e.getClass().getName() + ": " + e.getMessage());
                log.error("Failed to unmarshal contribution data XML, concorContributionId = {}", concorContributionId, e);
                eventService.logConcor(concorContributionId, FETCHED_FROM_MAAT, batchId, null, INTERNAL_SERVER_ERROR, "Failed to unmarshal contribution data XML");
                continue;
            }

            try {
                final var request = ConcorContributionReqForDrc.of(concorContributionId.intValue(), currentContribution);
                if (!feature.outgoingIsolated()) {
                    drcClient.sendConcorContributionReqToDrc(request);
                    log.info("Sent contribution data to DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
                } else {
                    log.info("Skipping contribution data to DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
                    final var json = objectMapper.writeValueAsString(request);
                    log.debug("Skipping contribution data to DRC, JSON = [{}]", json);
                }
                successfulContributions.put(concorContributionId.toString(), currentContribution);
                eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, OK, null);
            } catch (Exception e) {
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedContributions.put(concorContributionId.toString(), e.getClass().getName() + ": " + e.getMessage());
                eventService.logConcor(concorContributionId, SENT_TO_DRC, batchId, currentContribution, INTERNAL_SERVER_ERROR, "Failed to send contribution data to DRC");
            }
        }
    }

    private Integer updateContributionsAndCreateFile(Map<String, CONTRIBUTIONS> successfulContributions, Map<String, String> failedContributions) {
        // If any contributions were sent, then create XML file:
        Integer contributionFileId = null;
        if (Objects.nonNull(successfulContributions) && !successfulContributions.isEmpty()) {
            // Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            LocalDateTime dateGenerated = LocalDateTime.now();
            String fileName = contributionsMapperUtils.generateFileName(dateGenerated);
            String xmlFile = contributionsMapperUtils.generateFileXML(successfulContributions.values().stream().toList(), fileName);
            String ackXml = contributionsMapperUtils.generateAckXML(fileName, dateGenerated.toLocalDate(), failedContributions.size(), successfulContributions.size());
            List<String> successfulIdList = successfulContributions.keySet().stream().toList();
            try {
                contributionFileId = contributionUpdateRequest(xmlFile, successfulIdList, successfulIdList.size(), fileName, ackXml);
                log.info("Created Concor contribution-file ID {} from {} Concor contribution IDs [{}]", contributionFileId, successfulIdList.size(), String.join(", ", successfulIdList));
                // Explicitly log the Concor contribution IDs that were updated:
                for(Map.Entry<String, CONTRIBUTIONS> contribEntry: successfulContributions.entrySet()){
                    eventService.logConcor(new BigInteger(contribEntry.getKey()), UPDATED_IN_MAAT, batchId, contribEntry.getValue(), OK, null);
                }
            } catch (MaatApiClientException | WebClientResponseException | HttpServerErrorException e) {
                // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
                String payload = "Failed to create Concor contribution-file. Investigation needed. State of files will be out of sync! [" + e.getClass().getName() + "(" + e.getMessage() + ")]";
                // If failed, we want to handle this. As it will mean the whole process failed for current day.
                // TODO: Need to figure how we're going to log a failed call to the ATOMIC UPDATE.
                eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, INTERNAL_SERVER_ERROR, payload);
                throw e;
            }
        }
        logMaatUpdateEvents(successfulContributions, failedContributions);

        return contributionFileId;
    }

    @Retry(name = SERVICE_NAME)
    public Integer contributionUpdateRequest(String xmlContent, List<String> concorContributionIdList, int numberOfRecords, String fileName, String fileAckXML) throws HttpServerErrorException {
        ContributionUpdateRequest request = ContributionUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .concorContributionIds(concorContributionIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        if (!feature.outgoingIsolated()) {
            return contributionClient.updateContributions(request);
        } else {
            return 0;
        }
    }

    private void logMaatUpdateEvents(Map<String, CONTRIBUTIONS> successfulContributions, Map<String, String> failedContributions) {
        // log success and failure numbers.
        eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, OK, "Successfully Sent:"+ successfulContributions.size());
        eventService.logConcor(null, UPDATED_IN_MAAT, batchId, null, (failedContributions.size()>0?INTERNAL_SERVER_ERROR:OK), "Failed To Send:"+ failedContributions.size());
        for(Map.Entry<String, CONTRIBUTIONS> currentContribution: successfulContributions.entrySet()){
            eventService.logConcor(new BigInteger(currentContribution.getKey()),UPDATED_IN_MAAT, batchId, currentContribution.getValue(), OK, null);
        }
    }
}
