package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseMigrationEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseMigrationRepository;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ObjectFactory;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType.CONTRIBUTION;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType.FDC;

@Service
@Slf4j
public class MigrationService {
    private static final String DIVIDER = "------------------------------------------------------------";
    private static final Integer NUM_CONTRIBUTION_THREADS = 2;
    public static final int CONTRIBUTION_GET_MAX_NUMBER = 350;
    public static final int FDC_GET_MAX_NUMBER = 1000;
    private final DrcClient drcClient;
    private final FdcClient fdcClient;
    private final ContributionClient contributionClient;
    private final CaseMigrationRepository caseMigrationRepository;
    public final List<Unmarshaller> contributionUnmarshallerList;
    private final FeatureProperties feature;
    private final AnonymisingDataService anonymisingDataService;
    private final FdcMapperUtils fdcMapperUtils;

    @Autowired
    public MigrationService(DrcClient drcClient, FdcClient fdcClient, ContributionClient contributionClient, CaseMigrationRepository caseMigrationRepository, FeatureProperties feature, AnonymisingDataService anonymisingDataService, FdcMapperUtils fdcMapperUtils) throws JAXBException {
        this.drcClient = drcClient;
        this.fdcClient = fdcClient;
        this.contributionClient = contributionClient;
        this.caseMigrationRepository = caseMigrationRepository;
        this.feature = feature;
        this.anonymisingDataService = anonymisingDataService;
        this.fdcMapperUtils = fdcMapperUtils;
        this.contributionUnmarshallerList = new ArrayList<>();
        createUnmarshallerList();
    }

    /**
     * Main function of the migration process.
     * This will create 4 threads: by type and batch id i.e. Concor+Odd/Concor+Even/Fdc+Odd/Fdc+Even
     * This process will check for batches in the DCES Migration table.
     * Then for each batch:
     * <ol>
     * <li>Get the fdc/concor entries from Maat API for the batch</li>
     * <li>Send each to the DRC</li>
     * <li>Update the migration DB with details of the send to DRC ( success/fail/etc )</li>
     * </ol>
     * @throws InterruptedException InterruptedException Will be thrown if there is a thread exception.
     */
    public void migration() throws InterruptedException {
        LocalDateTime startTime = LocalDateTime.now();
        Long maxBatch = getMaxBatch();
        int numProcessed = 0;

        ExecutorService executorService =
                new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>());
        List<Callable<Integer>> callableList = getCallableList(maxBatch);
        List<Future<Integer>> futures = executorService.invokeAll(callableList);
        executorService.shutdown();

        for( Future<Integer> future : futures){
            try {
                numProcessed += future.get();
            } catch (ExecutionException e) {
                log.error("Exception in running! "+e.getClass().getSimpleName()+": "+e.getMessage());
            }
        }
        // Stats!
        LocalDateTime endTime = LocalDateTime.now();
        Duration d = Duration.between(startTime, endTime);
        double millisPerProcess = d.toMillis() / (double) numProcessed;
        log.info(DIVIDER);
        log.info(DIVIDER);
        log.info("Process Completed in:{}h:{}m:{}s:{}ms",d.toHoursPart(), d.toMinutesPart(),d.toSecondsPart(),d.toMillisPart());
        log.info("Processed: {}, at {}ms per entry", numProcessed, millisPerProcess);
        log.info(DIVIDER);
        log.info("Thread 1:{}",futures.get(0));
        log.info("Thread 2:{}",futures.get(1));
        log.info("Thread 3:{}",futures.get(2));
        log.info("Thread 4:{}",futures.get(3));
        log.info(DIVIDER);
        log.info(DIVIDER);
    }

    /**
     * Method to iterate through the batches. Parameters are for forming the for loop.
     * i.e. for(long l=<b>startBatch</b>; l<<b>maxBatch</b>; l+=<b>incrementingNumber</b>)
     * @param startBatch long The starting batch number. E.g. 1 will start it processing at batch 1.
     * @param maxBatch long The end batch number. Highest batch number it will attempt to process.
     * @param incrementingNumber long Amount to increment. E.g. 2 will mean it process every other number.
     * @return Integer The total number of entries processed by the loop.
     */
    public Integer fdcMigrationLoop(long startBatch, long maxBatch, int incrementingNumber){
        int numProcessed = 0;
        for(long batchNumber=startBatch; batchNumber<=maxBatch; batchNumber+=incrementingNumber) {
            numProcessed += migrateFdcEntries(batchNumber);
        }
        return numProcessed;
    }

    /**
     * Method to iterate through the batches. Parameters are for forming the for loop.
     * i.e. for(long l=<b>startBatch</b>; l<<b>maxBatch</b>; l+=<b>incrementingNumber</b>)
     * @param startBatch long The starting batch number. E.g. 1 will start it processing at batch 1.
     * @param maxBatch long The end batch number. Highest batch number it will attempt to process.
     * @param incrementingNumber long Amount to increment. E.g. 2 will mean it process every other number.
     * @param unmarshaller Unmarshaller The unmarshaller used to turn XML->Objects. Unmarshallers aren't threadsafe, so each thread needs it's own.
     * @return Integer The total number of entries processed by the loop.
     */
    public Integer contributionMigrationLoop(long startBatch, long maxBatch, int incrementingNumber, Unmarshaller unmarshaller){
        int numProcessed = 0;
        for(long batchNumber=startBatch; batchNumber<=maxBatch; batchNumber+=incrementingNumber) {
            numProcessed += migrateConcorEntries(batchNumber, unmarshaller);
        }
        return numProcessed;
    }

    /**
     * Method to send all of a single batch number to the DRC.
     * Process flow:
     * <ul>
     * <li>Get maatId/FdcId entries for the batch id given. Filtering on RecordType=Fdc, isProcessed=false</li>
     * <li>Query the Maat API with the list of FdcIds from above to get list of actual FDC data</li>
     * <li>Iterate through batch, sending each fdc to the DRC. Saving information about the success/fail back to the DB</li>
     * </ul>
     * @param batchNumber long The batch that is to be processed.
     * @return Integer The number of entries that were sent to the DRC.
     */
    public Integer migrateFdcEntries(long batchNumber){
        log.info("Fdc Batch:{} entered",batchNumber);
        int numProcessed = 0;
        List<CaseMigrationEntity> batchResults = caseMigrationRepository.getCaseMigrationEntitiesByBatchIdAndRecordTypeAndIsProcessed(batchNumber, FDC.getName(), false);
        log.info("Fdc Batch:{} to process: {}",batchNumber,batchResults.size());
        Map<Long, Fdc> fdcMap = new HashMap<>();
        try {
            fdcMap = getFdcsForMigration(batchResults);
        } catch (Exception e){
            handleBatchError(batchNumber, "Fdc", e);
        }
        for (CaseMigrationEntity currentMigrationEntity : batchResults) {
            Fdc currentFdc;
            try {
                currentFdc = fdcMap.get(currentMigrationEntity.getFdcId());
                if( !feature.outgoingIsolated() ) {
                    var request = FdcReqForDrc.of(currentFdc.getId().intValue(), currentFdc, Map.of("migrated", "true"));
                    drcClient.sendFdcReqToDrc(request);
                }
                currentMigrationEntity.setProcessed(true);
                currentMigrationEntity.setProcessedDate(LocalDateTime.now());
            } catch (WebClientResponseException e) {
                handleException(currentMigrationEntity, e);
            }
            caseMigrationRepository.save(currentMigrationEntity);
            numProcessed++;
        }
        log.info("Fdc Batch {} processed: {}",batchNumber, numProcessed);
        return numProcessed;
    }

    /**
     * Method to send all of a single batch number to the DRC.
     * Process flow:
     * <ul>
     * <li>Get maatId/ConcorId entries for the batch id given. Filtering on RecordType=Contribution, isProcessed=false</li>
     * <li>Query the Maat API with the list of FdcIds from above to get list of actual FDC data</li>
     * <li>Iterate through batch, sending each fdc to the DRC. Saving information about the success/fail back to the DB</li>
     * </ul>
     * @param batchNumber long The batch that is to be processed.
     * @param unmarshaller Unmarshaller The unmarshaller to be used for the XML->Object conversion of the concorData.
     * @return Integer The number of entries that were sent to the DRC.
     */
    public Integer migrateConcorEntries(long batchNumber, Unmarshaller unmarshaller){
        log.info("Contribution Batch:{} entered",batchNumber);
        int numProcessed = 0;
        List<CaseMigrationEntity> batchResults = caseMigrationRepository.getCaseMigrationEntitiesByBatchIdAndRecordTypeAndIsProcessed(batchNumber, CONTRIBUTION.getName(), false);
        log.info("Contribution Batch:{} to process: {}",batchNumber,batchResults.size());
        Map<Long, String> concorMap = new HashMap<>();
        try {
            concorMap = getContributionsForMigration(batchResults);
        } catch (Exception e){
            handleBatchError(batchNumber, "Contribution", e);
        }
        for (CaseMigrationEntity currentMigrationEntity : batchResults){
            String currentConcorXML = concorMap.get(currentMigrationEntity.getConcorContributionId());
            if (Objects.isNull(currentConcorXML) || currentConcorXML.isEmpty()){
                log.error("Value not found: maatId:{}, concorId:{}", currentMigrationEntity.getMaatId(), currentMigrationEntity.getConcorContributionId());
                continue;
            }
            try {
                CONTRIBUTIONS contribution = mapToContribution(currentConcorXML, unmarshaller);
                if( !feature.outgoingIsolated() ) {
                    var request = ConcorContributionReqForDrc.of(currentMigrationEntity.getConcorContributionId(), contribution, Map.of("migrated", "true"));
                    drcClient.sendConcorContributionReqToDrc(request);
                }
                currentMigrationEntity.setProcessed(true);
                currentMigrationEntity.setProcessedDate(LocalDateTime.now());
            } catch (WebClientResponseException e) {
                handleException(currentMigrationEntity, e);
            } catch (Exception e) {
                handleException(currentMigrationEntity, e);
            }
            caseMigrationRepository.save(currentMigrationEntity);
            numProcessed++;
        }
        log.info("Contribution Batch {} processed: {}",batchNumber, numProcessed);
        return numProcessed;
    }

    private Map<Long, Fdc> getFdcsForMigration(List<CaseMigrationEntity> batchResults){
        // if empty, just move on.
        if(batchResults.isEmpty()){
            return new HashMap<>();
        }
        List<Long> concorIdList = batchResults.stream().map(CaseMigrationEntity::getFdcId).toList();
        List<FdcContributionEntry> fdcList = new ArrayList<>();

        // 0->1000 = 1000 values. Then on next loop. 1000->2000 = 1000 values
        int upperIndex = FDC_GET_MAX_NUMBER;
        int lowerIndex = 0;
        while (lowerIndex<concorIdList.size()){
            List<Long> currentIdList = concorIdList.subList(lowerIndex, Math.min(upperIndex,concorIdList.size()));
            fdcList.addAll(fdcClient.getFdcListById(currentIdList).getFdcContributions());
            lowerIndex += FDC_GET_MAX_NUMBER;
            upperIndex += FDC_GET_MAX_NUMBER;
        }
        if(fdcList.size()!=concorIdList.size()) {
            log.error("Not all values were obtained! Expected:{}, Actual:{}", concorIdList.size(), fdcList.size());
        }
        return fdcList.stream().collect(Collectors.toMap(FdcContributionEntry::getId, fdcMapperUtils::mapFdcEntry));
    }

    private Map<Long, String> getContributionsForMigration(List<CaseMigrationEntity> batchResults){
        // if empty, just move on.
        if(batchResults.isEmpty()){
            return new HashMap<>();
        }
        // get concorIdList
        List<Long> concorIdList = batchResults.stream().map(CaseMigrationEntity::getConcorContributionId).toList();
        List<ConcorContribEntry> contributionList = new ArrayList<>();

        // 0->350 = 350 values. Then on next loop. 350->700
        int upperIndex = CONTRIBUTION_GET_MAX_NUMBER;
        int lowerIndex = 0;
        while (lowerIndex<concorIdList.size()){
            List<Long> currentIdList = concorIdList.subList(lowerIndex,Math.min(upperIndex,concorIdList.size()));
            contributionList.addAll(contributionClient.getConcorListById(currentIdList));
            lowerIndex += CONTRIBUTION_GET_MAX_NUMBER;
            upperIndex += CONTRIBUTION_GET_MAX_NUMBER;
        }
        if(contributionList.size()!=concorIdList.size()) {
            log.error("Not all values were obtained! Expected:{}, Actual:{}", concorIdList.size(), contributionList.size());
        }
        return contributionList.stream().collect(Collectors.toMap(ConcorContribEntry::getConcorContributionId, ConcorContribEntry::getXmlContent));
    }

    // Helpers

    public Long getMaxBatch(){
        Optional<Long> maxBatchOptional = caseMigrationRepository.getHighestBatchNumber();
        return maxBatchOptional.orElse(0L);
    }

    private void createUnmarshallerList() throws JAXBException {
        for(int i=0; i<NUM_CONTRIBUTION_THREADS; i++){
            JAXBContext jaxbContext;
            try {
                jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
                this.contributionUnmarshallerList.add(jaxbContext.createUnmarshaller());
            } catch (JAXBException e) {
                log.error("Error Creating Unmarshaller List!");
                throw e;
            }
        }
    }

    private List<Callable<Integer>> getCallableList(Long maxBatch){
        List<Callable<Integer>> callableList = new ArrayList<>();
        callableList.add(() -> fdcMigrationLoop(0L, maxBatch, 2));
        callableList.add(() -> fdcMigrationLoop(1L, maxBatch, 2));
        callableList.add(() -> contributionMigrationLoop(0L, maxBatch, 2, contributionUnmarshallerList.get(0)));
        callableList.add(() -> contributionMigrationLoop(1L, maxBatch, 2, contributionUnmarshallerList.get(1)));
        return callableList;
    }

    private CONTRIBUTIONS mapToContribution(String currentConcorXml, Unmarshaller unmarshaller) throws JAXBException {
        JAXBElement<CONTRIBUTIONS> convertedXml = unmarshaller.unmarshal(new StreamSource(new StringReader(currentConcorXml)),CONTRIBUTIONS.class);
        CONTRIBUTIONS contribution = convertedXml.getValue();

        if (feature.outgoingAnonymized()) {
            log.info("Feature:OutgoingAnonymized: contribution data will be anonymized.");
            contribution = anonymisingDataService.anonymise(contribution);
        }
        return contribution;
    }

    // Exception Handling
    private void handleException(CaseMigrationEntity migrationEntity, WebClientResponseException e){
        migrationEntity.setProcessed(true);
        migrationEntity.setProcessedDate(LocalDateTime.now());
        migrationEntity.setHttpStatus(e.getStatusCode().value());
        migrationEntity.setPayload(e.getMessage());
    }
    private void handleException(CaseMigrationEntity migrationEntity, Exception e){
        migrationEntity.setProcessed(true);
        migrationEntity.setProcessedDate(LocalDateTime.now());
        migrationEntity.setHttpStatus(500);
        migrationEntity.setPayload(String.format("Exception Encountered: %s Message: %s",e.getClass().getSimpleName(),e.getMessage()));
    }

    private void handleBatchError(long batchId, String type, Exception e){
        log.error("Exception {} encountered for {} batch:{} | Message:{} | Stack: {}", e.getClass().getSimpleName(), type, batchId, e.getMessage(), e.getStackTrace());
    }
}
