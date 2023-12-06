package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionPutRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

import java.math.BigInteger;
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
            String xmlFile = contributionsMapperUtils.generateFileXML(successfulContributions);
            // TODO: Construct other parameters for the "ATOMIC UPDATE" call.
            // populate the list of successful IDS from the successful contributions.
            List<String> successfulIdList = successfulContributions.stream()
                    .filter(Objects::nonNull)  // null safety.
                    .map(contribution -> contribution.getId().toString())// we only care for the id
                    .toList();

            ContributionPutRequest(xmlFile, successfulIdList, successfulIdList.size());
            // Failed XML lines to be logged. Need to use this to set the ATOMIC UPDATE's ack field.
            if(!failedContributions.isEmpty()){
                log.info("Contributions failed to send: {}", failedContributions.size());
            }



            // TODO: Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            fileSentSuccess = Objects.nonNull(xmlFile);

            // TODO: If failed, we want to handle this. As it will mean the whole process failed for current day.

        }
        // TODO: Need to figure how we're going to log a failed call to the ATOMIC UPDATE.

        return fileSentSuccess;
    }

    private Boolean ContributionPutRequest(String xmlContent, List<String> concurContributionIdList, int numberOfRecords){
        return contributionClient.updateContributions(new ContributionPutRequest(xmlContent,concurContributionIdList,numberOfRecords));
    }
}
