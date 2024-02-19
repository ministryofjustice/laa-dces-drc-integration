package uk.gov.justice.laa.crime.dces.integration.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class FdcService implements FileService{

    public static final String REQUESTED_STATUS = "REQUESTED";
    private final FdcMapperUtils fdcMapperUtils;
    private final ContributionClient contributionClient;

    // TODO Change all Objects to the actual object type.

    public boolean processDailyFiles() throws WebClientResponseException {
        FdcGlobalUpdateResponse globalUpdateResponse = callFdcGlobalUpdate();

        if ( !globalUpdateResponse.isSuccessful()) {
            // We've failed to do a global update. Raise as prio.
            log.error("Fdc Global Update failed!");
            // TODO: throw custom error
            return false;
        }
        // get all the potential values via maat call
        List<Fdc> fdcList = getFdcList();

        List<Fdc> successfulFdcs = new ArrayList<>();
        Map<String,String> failedContributions = new HashMap<>();

        // for each contribution sent by MAAT API
        for ( Fdc currentFdc : fdcList) {

            // TODO: Send Contribution to DRC on line below:
            boolean updateSuccessful = true; // hook in drc call here.
            // handle response
            // if successful/failure track accordingly.
            if (updateSuccessful){
                // If successful, we need to track that we have sent this, as it will form part of the XMLFile, and
                // needs it's status to "sent" in MAAT.
                successfulFdcs.add(currentFdc);
            }
            else{
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedContributions.put("currentFdc", "failure reason");
            }

        }
        // if >1 contribution was sent
        // create xml file
        boolean fileSentSuccess = false;
        if ( Objects.nonNull(successfulFdcs) && !successfulFdcs.isEmpty() ) {
            // TODO Create FDC xml File.
            String xmlFile = fdcMapperUtils.generateFileXML(successfulFdcs);
            // TODO: Construct other parameters for the "ATOMIC UPDATE" call.
            // populate the list of successful IDS from the successful contributions.
            List<BigInteger> successfulIdList = successfulFdcs.stream()
                    .filter(Objects::nonNull)
                    .map(Fdc::getId)
                    .toList();
            // check numbers.

            logNumberDiscepancies(globalUpdateResponse.getNumberOfUpdates(), fdcList.size(), successfulFdcs.size());
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

    void logNumberDiscepancies(int globalUpdateCount, int getFdcCount, int successfullySentFdcCount){
        if ( globalUpdateCount != getFdcCount ){
            log.info("Fdc number discrepancy: {} affected by global update, {} from getFdcs", globalUpdateCount, getFdcCount);
        }
        if ( getFdcCount != successfullySentFdcCount ){
            log.info("Fdc number discrepancy: {} from getFdcs, {} successfully sent", getFdcCount, successfullySentFdcCount);
        }
    }

    List<Fdc> getFdcList() throws HttpServerErrorException{
        FdcContributionsResponse response;
        try {
            response = contributionClient.getFdcContributions(REQUESTED_STATUS);
        }
        catch ( HttpServerErrorException e ) {
            log.error("Fdc Get Failed. The Global Update was successful!");
            throw e;
        }
        List<Fdc> fdcList = new ArrayList<>();
        if (Objects.nonNull(response)
                && Objects.nonNull(response.getFdcContributions())
                && !response.getFdcContributions().isEmpty()) {
            List<FdcContributionEntry> fdcContributionEntryList= response.getFdcContributions();
            fdcList = fdcContributionEntryList.stream().map(fdcMapperUtils::mapFdcEntry).toList();
        }
        return fdcList;
    }


    private FdcGlobalUpdateResponse callFdcGlobalUpdate(){
        try {
            return contributionClient.executeFdcGlobalUpdate();
        }
        catch (WebClientResponseException e){
            log.error("Fdc Gloabl Update threw an exception");
            throw e;
        }
    }

}
