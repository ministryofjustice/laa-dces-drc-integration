package uk.gov.justice.laa.crime.dces.integration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.ObjectFactory;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class FdcService implements FileService{

    private final FdcMapperUtils fdcMapperUtils;

    public FdcService(FdcMapperUtils fdcMapperUtils) {
        this.fdcMapperUtils = fdcMapperUtils;
    }
    // TODO Change all Objects to the actual object type.

    public boolean processDailyFiles() {
        // TODO: Call FDC global update

        List<Fdc> fdcList = null;
        List<Fdc> successfulFdcs = new ArrayList<>();
        Map<String,String> failedContributions = new HashMap<>();
        // get all the potential values via maat call
        // TODO: MAAT API "Get Active Fdc" REST call on line below:
        fdcList = new ArrayList<>();
        ObjectFactory of = new ObjectFactory();
        fdcList.add(of.createFdcFileFdcListFdc());
        //

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
//            List<BigInteger> successfulIdList = successfulFdcs.stream()
//                    .filter(Objects::nonNull)  // null safety.
//                    .map(CONTRIBUTIONS::getId) // we only care for the id
//                    .toList();
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

}
