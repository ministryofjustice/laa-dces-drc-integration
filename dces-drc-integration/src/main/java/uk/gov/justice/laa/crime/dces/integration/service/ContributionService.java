package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.Feature;
import uk.gov.justice.laa.crime.dces.integration.enums.ContributionRecordStatus;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

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

    public Integer processContributionUpdate(UpdateLogContributionRequest updateLogContributionRequest) {
        try {
            if (!feature.incomingIsolated()) {
                return contributionClient.sendLogContributionProcessed(updateLogContributionRequest);
            } else {
                return 0; // avoid updating MAAT DB.
            }
        } catch (MaatApiClientException | WebClientResponseException | HttpServerErrorException e) {
            log.info("Failed to processContributionUpdate", e);
            throw e;
        }
    }

    @Timed(value = "laa_dces_drc_service_process_contributions_daily_files",
            description = "Time taken to process the daily contributions files from DRC and passing this for downstream processing.")
    public boolean processDailyFiles() {
        int defaultNoOfRecord = feature.noOfContributionRecords();
        List<ConcorContribEntry> contributionsList;
        int startingId = 0;
        boolean hasProcessedAnyRecords = false;
        do {
            Map<String, CONTRIBUTIONS> successfulContributions = new HashMap<>();
            Map<String, String> failedContributions = new HashMap<>();
            contributionsList = contributionClient.getContributions(ContributionRecordStatus.ACTIVE.name(), startingId, defaultNoOfRecord);
            if (contributionsList != null && !contributionsList.isEmpty()) {
                sendContributionsToDrc(contributionsList, successfulContributions, failedContributions);
                Integer contributionFileId = updateContributionsAndCreateFile(successfulContributions, failedContributions);
                log.info("Created contribution-file ID {}", contributionFileId);
                startingId = contributionsList.get(contributionsList.size() - 1).getConcorContributionId();
                hasProcessedAnyRecords = true;
            }
        } while (contributionsList != null && contributionsList.size() == defaultNoOfRecord);

        return hasProcessedAnyRecords;
    }


    @Retry(name = SERVICE_NAME)
    public void sendContributionsToDrc(List<ConcorContribEntry> contributionsList, Map<String, CONTRIBUTIONS> successfulContributions, Map<String, String> failedContributions) {
        // for each contribution sent by MAAT API
        for (ConcorContribEntry contribEntry : contributionsList) {
            final int concorContributionId = contribEntry.getConcorContributionId();
            // convert string into objects
            CONTRIBUTIONS currentContribution;
            try {
                currentContribution = contributionsMapperUtils.mapLineXMLToObject(contribEntry.getXmlContent());
                if (feature.outgoingAnonymized()) {
                    // anonymize the data when flag is true - only for non production environments
                    currentContribution = anonymisingDataService.anonymise(currentContribution);
                }
            } catch (JAXBException e) {
                log.error("Failed to unmarshal contribution data XML, concorContributionId = {}", concorContributionId, e);
                failedContributions.put(Integer.toString(concorContributionId), e.getClass().getName() + ": " + e.getMessage());
                continue;
            }

            try {
                final var request = ConcorContributionReqForDrc.of(concorContributionId, currentContribution);
                if (!feature.outgoingIsolated()) {
                    drcClient.sendConcorContributionReqToDrc(request);
                    log.info("Sent contribution data to DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
                } else {
                    log.info("Skipping contribution data to DRC, concorContributionId = {}, maatId = {}", concorContributionId, currentContribution.getMaatId());
                    final var json = objectMapper.writeValueAsString(request);
                    log.debug("Skipping contribution data to DRC, JSON = [{}]", json);
                }
                successfulContributions.put(Integer.toString(concorContributionId), currentContribution);
            } catch (Exception e) {
                log.warn("Failed to send contribution data to DRC, concorContributionId = {}", concorContributionId, e);
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedContributions.put(Integer.toString(concorContributionId), e.getClass().getName() + ": " + e.getMessage());
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

            // Failed XML lines to be logged. Need to use this to set the ATOMIC UPDATE's ack field.
            if (!failedContributions.isEmpty()) {
                log.info("Failed to send {} Concor contributions", failedContributions.size());
            }
            try {
                contributionFileId = contributionUpdateRequest(xmlFile, successfulIdList, successfulIdList.size(), fileName, ackXml);
                // Explicitly log the Concor contribution IDs that were updated:
                log.info("Created Concor contribution-file ID {} from {} Concor contribution IDs [{}]", contributionFileId, successfulIdList.size(), String.join(", ", successfulIdList));
            } catch (MaatApiClientException | WebClientResponseException | HttpServerErrorException e) {
                // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
                log.error("Failed to create Concor contribution-file. Investigation needed. State of files will be out of sync! [" + e.getClass().getName() + "(" + e.getMessage() + ")]");
                // If failed, we want to handle this. As it will mean the whole process failed for current day.
                // TODO: Need to figure how we're going to log a failed call to the ATOMIC UPDATE.
                throw e;
            }
        }
        return contributionFileId;
    }

    @Retry(name = SERVICE_NAME)
    public Integer contributionUpdateRequest(String xmlContent, List<String> concorContributionIdList, int numberOfRecords, String fileName, String fileAckXML) throws HttpServerErrorException {
        log.info("Sending contribution update request to MAAT API for {}", concorContributionIdList.stream().peek(log::info).toList());
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
}
