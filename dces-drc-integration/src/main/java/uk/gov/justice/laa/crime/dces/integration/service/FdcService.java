package uk.gov.justice.laa.crime.dces.integration.service;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.SendFdcFileDataToDrcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import java.math.BigInteger;
import java.time.LocalDateTime;
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
    private final DrcClient drcClient;

    public String processFdcUpdate(UpdateLogFdcRequest updateLogFdcRequest) {
        try {
            contributionClient.sendLogFdcProcessed(updateLogFdcRequest);
            return "The request has been processed successfully";
        } catch (MaatApiClientException | WebClientResponseException | HttpServerErrorException e) {
            log.info("processFdcUpdate failed", e);
            return "The request has failed to process";
        }
    }

    // TODO Change all Objects to the actual object type.
    @Timed(value = "laa_dces_drc_service_process_fdc_daily_files",
            description = "Time taken to process the daily fdc files from DRC and passing this for downstream processing.")
    public boolean processDailyFiles() throws WebClientResponseException {
        List<Fdc> successfulFdcs = new ArrayList<>();
        Map<String,String> failedFdcs = new HashMap<>();


        int globalUpdateResult = callGlobalUpdate();
        List<Fdc> fdcList = getFdcList();
        sendFdcToDrc(fdcList, successfulFdcs, failedFdcs);
        // check numbers.
        logNumberDiscepancies(globalUpdateResult, fdcList.size(), successfulFdcs.size());
        return updateFdcAndCreateFile(successfulFdcs, failedFdcs);
    }
    @SuppressWarnings("squid:S2583") // ignore the can only be true warning. As this is placeholder.
    private void sendFdcToDrc(List<Fdc> fdcList, List<Fdc> successfulFdcs, Map<String,String> failedFdcs){
        fdcList.forEach(currentFdc -> {
            Boolean updateSuccessful = drcClient.sendFdcUpdate(buildSendFdcFileDataToExternalRequest(currentFdc.getId().intValue()));
            if (Boolean.TRUE.equals(updateSuccessful)) {
                successfulFdcs.add(currentFdc);
                log.info("FDC update successful for ID: {}", currentFdc.getId());
            } else {
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedFdcs.put(String.valueOf(currentFdc.getId()), "failure reason");
                log.warn("FDC update failed for ID: {}. Reason: {}", currentFdc.getId(), "failure reason");
            }
        });
    }

    private SendFdcFileDataToDrcRequest buildSendFdcFileDataToExternalRequest(Integer fdcId) {
        return SendFdcFileDataToDrcRequest.builder()
                .fdcId(fdcId)
                .build();
    }

    private boolean updateFdcAndCreateFile(List<Fdc> successfulFdcs, Map<String,String> failedFdcs){
        // if >1 contribution was sent
        // finish off with updates and create the file.
        Integer contributionFileId = null;
        if ( Objects.nonNull(successfulFdcs) && !successfulFdcs.isEmpty() ) {
            // Construct other parameters for the "ATOMIC UPDATE" call.
            LocalDateTime dateGenerated = LocalDateTime.now();
            String fileName = fdcMapperUtils.generateFileName(dateGenerated);
            String ackXml = fdcMapperUtils.generateAckXML(fileName, dateGenerated.toLocalDate(), failedFdcs.size(), successfulFdcs.size());
            String xmlFile = fdcMapperUtils.generateFileXML(successfulFdcs);
            List<String> successfulIdList = successfulFdcs.stream()
                    .map(Fdc::getId)
                    .filter(Objects::nonNull)
                    .map(BigInteger::toString).toList();

            // Failed XML lines to be logged. Need to use this to set the ATOMIC UPDATE's ack field.
            if (!failedFdcs.isEmpty()) {
                log.info("Contributions failed to send: {}", failedFdcs.size());
            }
            // Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            try {
                contributionFileId = fdcUpdateRequest(xmlFile, successfulIdList, successfulIdList.size(), fileName, ackXml);
            } catch (MaatApiClientException | WebClientResponseException| HttpServerErrorException e){
                // TODO: If failed, we want to handle this. As it will mean the whole process failed for current day.
                log.error("Fdc file failed to send! Investigation needed.");
                throw e;
                // TODO: Need to figure how we're going to log a failed call to the ATOMIC UPDATE.
            }
        }
        return contributionFileId != null;
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
        catch (HttpServerErrorException e){
            log.error("Fdc Global Update threw an exception");
            throw e;
        }
    }


    private int callGlobalUpdate(){
        FdcGlobalUpdateResponse globalUpdateResponse;
        try {
            globalUpdateResponse = callFdcGlobalUpdate();
        }
        catch (Exception e){
            return logGlobalUpdateFailure(e.getMessage());
        }
        // handle response
        if(!globalUpdateResponse.isSuccessful()){
            return logGlobalUpdateFailure("Failure Returned from Endpoint");
        }
        return globalUpdateResponse.getNumberOfUpdates();
    }

    private int logGlobalUpdateFailure(String errorMessage){
        // We've failed to do a global update.
        // TODO: Flag here for sentry
        log.error("FDC Global Update failure: {}", errorMessage);
        // continue processing, as can still have data to deal with.
        return 0;
    }

    private Integer fdcUpdateRequest(String xmlContent, List<String> fdcIdList, int numberOfRecords, String fileName, String fileAckXML) throws HttpServerErrorException {
        FdcUpdateRequest request = FdcUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .fdcIds(fdcIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        return contributionClient.updateFdcs(request);
    }


}
