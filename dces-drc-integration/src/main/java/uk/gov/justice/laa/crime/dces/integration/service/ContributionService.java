package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.SendContributionFileDataToExternalRequest;
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

    private final ContributionsMapperUtils contributionsMapperUtils;
    private final ContributionClient contributionClient;
    private final DrcClient drcClient;

    public String processContributionUpdate(UpdateLogContributionRequest updateLogContributionRequest) {
        Boolean response = contributionClient.sendLogContributionProcessed(updateLogContributionRequest);
        if (response != null && response) {
            return "The request has been processed successfully";
        } else {
            return "The request has failed to process";
        }
    }

    public boolean processDailyFiles() {
        List<ConcurContribEntry> contributionsList;
        Map<String, CONTRIBUTIONS> successfulContributions = new HashMap<>();
        Map<String, String> failedContributions = new HashMap<>();
        // get all the values to process via maat call
        contributionsList = contributionClient.getContributions("ACTIVE");
        sendContributionsToDrc(contributionsList, successfulContributions, failedContributions);

        return updateContributionsAndCreateFile(successfulContributions, failedContributions);
    }

    private void sendContributionsToDrc(List<ConcurContribEntry> contributionsList, Map<String, CONTRIBUTIONS> successfulContributions, Map<String,String> failedContributions){
        // for each contribution sent by MAAT API
        for ( ConcurContribEntry contribEntry : contributionsList) {
            // convert string into objects
            CONTRIBUTIONS currentContribution;
            try {
                currentContribution = contributionsMapperUtils.mapLineXMLToObject(contribEntry.getXmlContent());
            } catch (JAXBException e) {
                log.error("Invalid line XML encountered");
                failedContributions.put(String.valueOf(contribEntry.getConcorContributionId()), "Invalid format.");
                continue;
            }

            String contributionId = String.valueOf(contribEntry.getConcorContributionId());
            Boolean updateSuccessful = drcClient.sendContributionUpdate(createDrcDataRequest(contribEntry));

            if (Boolean.TRUE.equals(updateSuccessful)) {
                successfulContributions.put(contributionId, currentContribution);
            } else {
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedContributions.put(contributionId, "failure reason");
            }
        }
    }

    private SendContributionFileDataToExternalRequest createDrcDataRequest(ConcurContribEntry contribEntry) {
        return SendContributionFileDataToExternalRequest.builder().contributionId(contribEntry.getConcorContributionId()).build();
    }

    private boolean updateContributionsAndCreateFile(Map<String, CONTRIBUTIONS> successfulContributions, Map<String,String> failedContributions){
        // if >1 contribution was sent
        // create xml file
        boolean fileSentSuccess = false;
        if ( Objects.nonNull(successfulContributions) && !successfulContributions.isEmpty() ) {
            // Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            LocalDateTime dateGenerated = LocalDateTime.now();
            String fileName = contributionsMapperUtils.generateFileName(dateGenerated);
            String xmlFile = contributionsMapperUtils.generateFileXML(successfulContributions.values().stream().toList(), fileName);
            String ackXml = contributionsMapperUtils.generateAckXML(fileName, dateGenerated.toLocalDate(), failedContributions.size(), successfulContributions.size());
            List<String> successfulIdList = successfulContributions.keySet().stream().toList();


            // Failed XML lines to be logged. Need to use this to set the ATOMIC UPDATE's ack field.
            if(!failedContributions.isEmpty()){
                log.info("Contributions failed to send: {}", failedContributions.size());
            }
            try {
                fileSentSuccess = contributionUpdateRequest(xmlFile, successfulIdList, successfulIdList.size(),fileName,ackXml);
            }
            catch (HttpServerErrorException e){
                // If failed, we want to handle this. As it will mean the whole process failed for current day.
                log.error("Contributions file failed to send! Investigation needed. State of files will be out of sync!");
                // TODO: Need to figure how we're going to log a failed call to the ATOMIC UPDATE.
                throw e;
            }
        }
        return fileSentSuccess;
    }

    private Boolean contributionUpdateRequest(String xmlContent, List<String> concurContributionIdList, int numberOfRecords, String fileName, String fileAckXML) throws HttpServerErrorException {
        ContributionUpdateRequest request = ContributionUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .concorContributionIds(concurContributionIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        return contributionClient.updateContributions(request);
    }

}
