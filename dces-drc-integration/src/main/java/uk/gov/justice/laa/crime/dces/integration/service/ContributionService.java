package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils;

import java.util.*;

@Service
@Slf4j
public class ContributionService {

    private final MapperUtils mapperUtils;

    public ContributionService(MapperUtils mapperUtils) {
        this.mapperUtils = mapperUtils;
    }

    public boolean processDailyFiles() {
        List<String> xmlStringList = null;
        List<CONTRIBUTIONS> successfulContributions = new ArrayList<>();
        Map<String,String> failedContributions = new HashMap<>();
        // get all the potential values via maat call
        xmlStringList = new ArrayList<>();
        //
        // for each xmlString
        for ( String xmlString : xmlStringList) {
            // convert string into objects
            CONTRIBUTIONS currentContribution = null;
            try {
                 currentContribution = mapperUtils.mapLineXMLToObject(xmlString);
            } catch (JAXBException e) {
                log.error("Invalid XML found");
                failedContributions.put(xmlString, "Invalid format.");
                continue;
            }

            // send contribution to drc
            boolean updateSuccessful = true; // hook in maatAPI call here.
            // handle response
            // if successful/failure track accordingly.
            if (updateSuccessful){
                successfulContributions.add(currentContribution);
            }
            else{
                failedContributions.put(xmlString, "failure reason");
            }

        }
        // if >1 contribution was sent
        // create xml file
        String xmlFile = mapperUtils.generateFileXML(successfulContributions);
        // send file
        boolean fileSentSuccess = Objects.nonNull(xmlFile);
        // handle failure here

        // handle/note the failed XML Lines
        if(!failedContributions.isEmpty()){

        }

        return fileSentSuccess;
    }

}
