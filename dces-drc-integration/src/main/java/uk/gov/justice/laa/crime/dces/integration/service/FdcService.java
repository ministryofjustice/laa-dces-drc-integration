package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.config.Feature;
import uk.gov.justice.laa.crime.dces.integration.datasource.CaseSubmissionService;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;
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

@RequiredArgsConstructor
@Service
@Slf4j
public class FdcService implements FileService {
    private static final String SERVICE_NAME = "FdcService";
    public static final String REQUESTED_STATUS = "REQUESTED";
    private final FdcMapperUtils fdcMapperUtils;
    private final FdcClient fdcClient;
    private final DrcClient drcClient;
    private final ObjectMapper objectMapper;
    private final Feature feature;
    private final CaseSubmissionService caseSubmissionService;
    private BigInteger batchId;

    public Integer processFdcUpdate(UpdateLogFdcRequest updateLogFdcRequest) {
        try {
            if (!feature.incomingIsolated()) {
                return fdcClient.sendLogFdcProcessed(updateLogFdcRequest);
            } else {
                return 0; // avoid updating MAAT DB.
            }
        } catch (MaatApiClientException | WebClientResponseException | HttpServerErrorException e) {
            log.info("Failed to processFdcUpdate", e);
            throw e;
        }


    }

    @Timed(value = "laa_dces_drc_service_process_fdc_daily_files",
            description = "Time taken to process the daily FDC files from DRC and passing this for downstream processing.")
    public boolean processDailyFiles() throws WebClientResponseException {
        batchId = caseSubmissionService.generateBatchId();
        List<Fdc> successfulFdcs = new ArrayList<>();
        Map<String,String> failedFdcs = new HashMap<>();
        int globalUpdateResult = callGlobalUpdate();
        List<Fdc> fdcList = getFdcList();
        sendFdcToDrc(fdcList, successfulFdcs, failedFdcs);
        // check numbers.
        logNumberDiscepancies(globalUpdateResult, fdcList.size(), successfulFdcs.size());
        return updateFdcAndCreateFile(successfulFdcs, failedFdcs) != null;
    }

    @Retry(name = SERVICE_NAME)
    void sendFdcToDrc(List<Fdc> fdcList, List<Fdc> successfulFdcs, Map<String,String> failedFdcs) {
        fdcList.forEach(currentFdc -> {
            // TODO: DB Log the fetch for each item
            int fdcId = currentFdc.getId().intValue();
            try {
                final var request = FdcReqForDrc.of(fdcId, currentFdc);
                if (!feature.outgoingIsolated()) {
                    drcClient.sendFdcReqToDrc(request);
                    log.info("Sent FDC data to DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
                } else {
                    log.info("Skipping FDC data to DRC, fdcId = {}, maatId = {}", fdcId, currentFdc.getMaatId());
                    final var json = objectMapper.writeValueAsString(request);
                    log.debug("Skipping FDC data to DRC, JSON = [{}]", json);
                }
                successfulFdcs.add(currentFdc);
                // TODO: DB Log sent to DRC, capture error etc.
            } catch (Exception e) {
                log.warn("Failed to send FDC data to DRC. fdcId = {}", fdcId, e);
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedFdcs.put(Integer.toString(fdcId), e.getClass().getName() + ": " + e.getMessage());
            }


        });
    }

    private Integer updateFdcAndCreateFile(List<Fdc> successfulFdcs, Map<String, String> failedFdcs) {
        // If any contributions were sent, then finish off with updates and create the file:
        Integer contributionFileId = null;
        if (Objects.nonNull(successfulFdcs) && !successfulFdcs.isEmpty()) {
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
                log.info("Failed to send {} FDC contributions", failedFdcs.size());
            }
            // Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            try {
                contributionFileId = fdcUpdateRequest(xmlFile, successfulIdList, successfulIdList.size(), fileName, ackXml);
                // Explicitly log the FDC contribution IDs that were updated:
                log.info("Created FDC contribution-file ID {} from {} FDC contribution IDs [{}]", contributionFileId, successfulIdList.size(), String.join(", ", successfulIdList));
            } catch (MaatApiClientException | WebClientResponseException| HttpServerErrorException e) {
                // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
                log.error("Failed to create FDC contribution-file. Investigation needed. State of files will be out of sync! [" + e.getClass().getName() + "(" + e.getMessage() + ")]");
                // If failed, we want to handle this. As it will mean the whole process failed for current day.
                // TODO: Need to figure how we're going to log a failed call to the ATOMIC UPDATE.
                throw e;
            }
        }
        // TODO: DB Log each successful id. I.e. iterate through list of successful ids, that have been updated. And create a new db logging line for each.
        return contributionFileId;
    }

    public void logNumberDiscepancies(int globalUpdateCount, int getFdcCount, int successfullySentFdcCount) {
        if (globalUpdateCount != getFdcCount) {
            log.info("FDC contribution count discrepancy: {} affected by FDC global update, {} from getFdcs", globalUpdateCount, getFdcCount);
        }
        if (getFdcCount != successfullySentFdcCount) {
            log.info("FDC contribution count discrepancy: {} from getFdcs, {} successfully sent", getFdcCount, successfullySentFdcCount);
        }
    }

    @Retry(name = SERVICE_NAME)
    public List<Fdc> getFdcList() throws HttpServerErrorException{
        FdcContributionsResponse response;
        try {
            response = fdcClient.getFdcContributions(REQUESTED_STATUS);
        } catch (HttpServerErrorException e) {
            // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
            log.error("Failed to retrieve FDC contributions, after the FDC global update completed [" + e.getClass().getName() + "(" + e.getMessage() + ")]");
            // TODO: DB Log event here if failed.
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

    @Retry(name = SERVICE_NAME)
    public FdcGlobalUpdateResponse callFdcGlobalUpdate(){
        try {
            if (!feature.outgoingIsolated()) {
                return fdcClient.executeFdcGlobalUpdate();
            } else {
                return new FdcGlobalUpdateResponse(true, 0);
            }
        } catch (HttpServerErrorException e) {
            // We're rethrowing the exception, therefore avoid logging the stack trace to prevent logging the same trace multiple times.
            log.error("FDC global update threw an exception [" + e.getClass().getName() + "(" + e.getMessage() + ")]");
            throw e;
        }
    }

    private int callGlobalUpdate() {
        FdcGlobalUpdateResponse globalUpdateResponse;
        try {
            globalUpdateResponse = callFdcGlobalUpdate();
        } catch (Exception e) {
            return logGlobalUpdateFailure(e.getMessage());
        }
        // handle response
        if (!globalUpdateResponse.isSuccessful()) {
            return logGlobalUpdateFailure("endpoint response has successful==false");
        }
        return globalUpdateResponse.getNumberOfUpdates();
    }

    private int logGlobalUpdateFailure(String errorMessage){
        // We've failed to do a global update.
        // TODO: Flag here for sentry
        log.error("Failed to complete FDC global update [{}]", errorMessage);
        // continue processing, as can still have data to deal with.
        return 0;
    }

    @Retry(name = SERVICE_NAME)
    public Integer fdcUpdateRequest(String xmlContent, List<String> fdcIdList, int numberOfRecords, String fileName, String fileAckXML) throws HttpServerErrorException {
        FdcUpdateRequest request = FdcUpdateRequest.builder()
                .recordsSent(numberOfRecords)
                .xmlContent(xmlContent)
                .fdcIds(fdcIdList)
                .xmlFileName(fileName)
                .ackXmlContent(fileAckXML).build();
        if (!feature.outgoingIsolated()) {
            return fdcClient.updateFdcs(request);
        } else {
            return 0;
        }
    }
}
