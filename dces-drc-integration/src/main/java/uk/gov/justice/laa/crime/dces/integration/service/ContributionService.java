package uk.gov.justice.laa.crime.dces.integration.service;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import jakarta.xml.bind.JAXBException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.SendContributionFileDataToDrcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class ContributionService implements FileService {
    private static final String SERVICE_NAME = "ContributionService";
    private final ContributionsMapperUtils contributionsMapperUtils;
    private final ContributionClient contributionClient;
    private final DrcClient drcClient;

    public String processContributionUpdate(UpdateLogContributionRequest updateLogContributionRequest) {
        try {
            contributionClient.sendLogContributionProcessed(updateLogContributionRequest);
            return "The request has been processed successfully";
        } catch (MaatApiClientException | WebClientResponseException | HttpServerErrorException e) {
            log.info("Failed to processContributionUpdate", e);
            return "The request has failed to process";
        }
    }

    @Timed(value = "laa_dces_drc_service_process_contributions_daily_files",
            description = "Time taken to process the daily contributions files from DRC and passing this for downstream processing.")
    public boolean processDailyFiles() {
        Map<String, CONTRIBUTIONS> successfulContributions = new HashMap<>();
        Map<String, String> failedContributions = new HashMap<>();
        // get all the values to process via maat call
        List<ConcurContribEntry> contributionsList = contributionClient.getContributions("ACTIVE");
        sendContributionsToDrc(contributionsList, successfulContributions, failedContributions);

        return updateContributionsAndCreateFile(successfulContributions, failedContributions) != null;
    }


    @Retry(name = SERVICE_NAME)
    public void sendContributionsToDrc(List<ConcurContribEntry> contributionsList, Map<String, CONTRIBUTIONS> successfulContributions, Map<String,String> failedContributions){
        // for each contribution sent by MAAT API
        for (ConcurContribEntry contribEntry : contributionsList) {
            final String contributionIdStr = String.valueOf(contribEntry.getConcorContributionId());
            // convert string into objects
            CONTRIBUTIONS currentContribution;
            try {
                currentContribution = contributionsMapperUtils.mapLineXMLToObject(contribEntry.getXmlContent());
            } catch (JAXBException e) {
                log.error("Failed to unmarshal XML for Concor contribution ID {}", contributionIdStr, e);
                failedContributions.put(contributionIdStr, e.getClass().getName() + ": " + e.getMessage());
                continue;
            }

            try {
                drcClient.sendContributionUpdate(createDrcDataRequest(currentContribution));
                log.info("Sent update to DRC for Concor contribution ID {}", contributionIdStr);
                successfulContributions.put(contributionIdStr, currentContribution);
            } catch (Exception e) {
                log.warn("Failed to send update to DRC for Concor contribution ID {}", contributionIdStr, e);
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedContributions.put(contributionIdStr, e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }

    private SendContributionFileDataToDrcRequest createDrcDataRequest(CONTRIBUTIONS contribution) {
        return SendContributionFileDataToDrcRequest.builder()
                .data(contribution)
                .build();
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
        ContributionUpdateRequest request = ContributionUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .concorContributionIds(concorContributionIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        return contributionClient.updateContributions(request);
    }
}
