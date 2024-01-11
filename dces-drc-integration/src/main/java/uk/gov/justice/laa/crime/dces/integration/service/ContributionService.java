package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionPutRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class ContributionService implements FileService{

    private final ContributionsMapperUtils contributionsMapperUtils;
    private final ContributionClient contributionClient;

    public boolean processDailyFiles() {
        List<ConcurContribEntry> contributionsList = null;
        List<CONTRIBUTIONS> successfulContributions = new ArrayList<>();
        Map<Integer,String> failedContributions = new HashMap<>();
        // get all the values to process via maat call
        contributionsList = contributionClient.getContributions("ACTIVE");
        List<String> successfulIdList = new ArrayList<>();
        // for each contribution sent by MAAT API
        for ( ConcurContribEntry contribEntry : contributionsList) {
            // convert string into objects
            CONTRIBUTIONS currentContribution = null;
            try {
                currentContribution = contributionsMapperUtils.mapLineXMLToObject(contribEntry.getXmlContent());
            } catch (JAXBException e) {
                log.error("Invalid line XML encountered");
                failedContributions.put(contribEntry.getConcorContributionId(), "Invalid format.");
                continue;
            }

            // TODO: Send Contribution to DRC on line below:
            boolean updateSuccessful = true; // hook in drc call here.
            // handle response
            // if successful/failure track accordingly.
            if (updateSuccessful){
                // If successful, we need to track that we have sent this, as it will form part of the XMLFile, and
                // needs it's status to "sent" in MAAT.
                successfulContributions.add(currentContribution);
                // populate the list of successful IDS from the successful contributions.
                successfulIdList.add(String.valueOf(contribEntry.getConcorContributionId()));
            }
            else{
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedContributions.put(contribEntry.getConcorContributionId(), "failure reason");
            }

        }
        // if >1 contribution was sent
        // create xml file
        boolean fileSentSuccess = false;
        if ( Objects.nonNull(successfulContributions) && !successfulContributions.isEmpty() ) {
            LocalDateTime dateGenerated = LocalDateTime.now();
            String fileName = contributionsMapperUtils.generateFileName(dateGenerated);
            String xmlFile = contributionsMapperUtils.generateFileXML(successfulContributions, fileName);
            String ackXml = contributionsMapperUtils.generateAckXML(fileName, dateGenerated.toLocalDate(), failedContributions.size(), successfulContributions.size());

            // Failed XML lines to be logged. Need to use this to set the ATOMIC UPDATE's ack field.
            if(!failedContributions.isEmpty()){
                log.info("Contributions failed to send: {}", failedContributions.size());
            }
            // Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            try {
                fileSentSuccess = contributionPutRequest(xmlFile, successfulIdList, successfulIdList.size(),fileName,ackXml);
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

    private Boolean contributionPutRequest(String xmlContent, List<String> concurContributionIdList, int numberOfRecords, String fileName, String fileAckXML) throws HttpServerErrorException {
        return contributionClient.updateContributions(new ContributionPutRequest(numberOfRecords, xmlContent,concurContributionIdList, fileName, fileAckXML));
    }
}
