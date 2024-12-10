package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBException;
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
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

import static uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType.CONTRIBUTION;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType.FDC;

@Service
@Slf4j
public class MigrationService {
    private final DrcClient drcClient;
    private final FdcClient fdcClient;
    private final ContributionClient contributionClient;
    private final CaseMigrationRepository caseMigrationRepository;
    private final ContributionsMapperUtils contributionsMapperUtils;
    private final FeatureProperties feature;
    private final AnonymisingDataService anonymisingDataService;

    @Autowired
    public MigrationService(DrcClient drcClient, FdcClient fdcClient, ContributionClient contributionClient, CaseMigrationRepository caseMigrationRepository, ContributionsMapperUtils contributionsMapperUtils, FeatureProperties feature, AnonymisingDataService anonymisingDataService){
        this.drcClient = drcClient;
        this.fdcClient = fdcClient;
        this.contributionClient = contributionClient;
        this.caseMigrationRepository = caseMigrationRepository;
        this.contributionsMapperUtils = contributionsMapperUtils;
        this.feature = feature;
        this.anonymisingDataService = anonymisingDataService;
    }

    private List<Callable<Integer>> getCallableList(Long maxBatch){
        List<Callable<Integer>> callableList = new ArrayList<>();
        callableList.add(() -> fdcMigrationLoop(0L, maxBatch, 2));
        callableList.add(() -> fdcMigrationLoop(1L, maxBatch, 2));
        callableList.add(() -> contributionMigrationLoop(0L, maxBatch, 2));
        callableList.add(() -> contributionMigrationLoop(1L, maxBatch, 2));
        return callableList;
    }

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
                log.error("Exception in running! "+e.getMessage());
            }
        }
        // Stats!
        LocalDateTime endTime = LocalDateTime.now();
        Duration d = Duration.between(startTime, endTime);
        double millisPerProcess = d.toMillis() / (double) numProcessed;
        // TODO Remove this message. For sake of ease of timing.
        log.info("------------------------------------------------------------");
        log.info("------------------------------------------------------------");
        log.info("Process Completed in:{}h:{}m:{}s:{}ms",d.toHoursPart(), d.toMinutesPart(),d.toSecondsPart(),d.toMillisPart());
        log.info("Processed: {}, at {}ms per entry", numProcessed, millisPerProcess);
        log.info("------------------------------------------------------------");
        log.info("------------------------------------------------------------");
    }

    public Integer fdcMigrationLoop(Long startBatch, Long maxBatch, int incrementingNumber){
        int numProcessed = 0;
        for(long batchNumber=startBatch; batchNumber<=maxBatch; batchNumber+=incrementingNumber) {
            numProcessed += migrateFdcEntries(batchNumber);
        }
        return numProcessed;
    }

    public Integer contributionMigrationLoop(Long startBatch, Long maxBatch, int incrementingNumber){
        int numProcessed = 0;
        for(long batchNumber=startBatch; batchNumber<=maxBatch; batchNumber+=incrementingNumber) {
            numProcessed += migrateConcorEntries(batchNumber);
        }
        return numProcessed;
    }




    public Long getMaxBatch(){
        Optional<Long> maxBatchOptional = caseMigrationRepository.getHighestBatchNumber();
        return maxBatchOptional.orElse(0L);
    }

    public Integer migrateConcorEntries(long batchNumber){
        List<CaseMigrationEntity> batchResults = caseMigrationRepository.getCaseMigrationEntitiesByBatchIdAndRecordTypeAndIsProcessed(batchNumber, CONTRIBUTION.getName(), false);
        int numProcessed = 0;

        for (CaseMigrationEntity currentMigrationEntity : batchResults){
            ConcorContribEntry currentConcor;
            try {
                currentConcor = contributionClient.getConcorById(currentMigrationEntity.getConcorContributionId());
                if (Objects.isNull(currentConcor)){
                    log.info("Value not found: maatId:{}, concorId:{}", currentMigrationEntity.getMaatId(), currentMigrationEntity.getConcorContributionId());
                    continue;
                }
                CONTRIBUTIONS contribution = mapToContribution(currentConcor, currentMigrationEntity);

                var request = ConcorContributionReqForDrc.of(currentConcor.getConcorContributionId(), contribution, Map.of("migrated", "true"));
//                drcClient.sendConcorContributionReqToDrc(request);
                log.info("Contribution Sent:"+currentConcor.getConcorContributionId());
                currentMigrationEntity.setProcessed(true);
                currentMigrationEntity.setProcessedDate(LocalDateTime.now());
            } catch (WebClientResponseException e) {
                handleException(currentMigrationEntity, e);
            } catch (JAXBException e) {
                handleException(currentMigrationEntity, e);
            }
            caseMigrationRepository.save(currentMigrationEntity);
            numProcessed++;
            // TODO: Break added here so we only deal with one value per cycle. To be removed.
//            log.info("DEDWARDS: Sent MaatId:{}, FdcId:{}",currentMigrationEntity.getMaatId(), currentMigrationEntity.getConcorContributionId());
            break;
        }
        return numProcessed;
    }

    public Integer migrateFdcEntries(long batchNumber){
        int numProcessed = 0;
        List<CaseMigrationEntity> batchResults = caseMigrationRepository.getCaseMigrationEntitiesByBatchIdAndRecordTypeAndIsProcessed(batchNumber, FDC.getName(), false);

        List<Long> fdcIdList = batchResults.stream().map(CaseMigrationEntity::getFdcId).toList();
//            List<Fdc> fdcList = fdcClient.getFdcsByIdList(fdcIdList);

        for (CaseMigrationEntity currentMigrationEntity : batchResults) {
            Fdc currentFdc;
            try {
                currentFdc = fdcClient.getFdcById(currentMigrationEntity.getFdcId()); // TODO Put back to fdc ID, this is due to test data error.
                var request = FdcReqForDrc.of(currentFdc.getId().intValue(), currentFdc, Map.of("migrated", "true"));
//                    drcClient.sendFdcReqToDrc(request);
                log.info("Fdc Sent:"+currentFdc.getId());
                currentMigrationEntity.setProcessed(true);
                currentMigrationEntity.setProcessedDate(LocalDateTime.now());
            } catch (WebClientResponseException e) {
                handleException(currentMigrationEntity, e);
            }
            caseMigrationRepository.save(currentMigrationEntity);
            numProcessed++;
            // TODO: Break added here so we only deal with one value per cycle. To be removed.
            break;
        }
        return numProcessed;
    }

    private void handleException(CaseMigrationEntity migrationEntity, WebClientResponseException e){
        migrationEntity.setProcessed(true);
        migrationEntity.setProcessedDate(LocalDateTime.now());
        migrationEntity.setHttpStatus(e.getStatusCode().value());
        migrationEntity.setPayload(e.getMessage());
    }
    private void handleException(CaseMigrationEntity migrationEntity, JAXBException e){
        migrationEntity.setProcessed(true);
        migrationEntity.setProcessedDate(LocalDateTime.now());
        migrationEntity.setHttpStatus(500);
        migrationEntity.setPayload(e.getMessage());
    }

    private CONTRIBUTIONS mapToContribution(ConcorContribEntry currentConcor, CaseMigrationEntity currentMigrationEntity) throws JAXBException {
        // TODO Handle the no values problem!
        if(Objects.isNull(currentConcor.getXmlContent())){
//            log.info("DEDWARDS: No XML was found for ID:{}", currentConcor.getConcorContributionId());
            CONTRIBUTIONS standInContribution = new CONTRIBUTIONS();
            standInContribution.setId(currentConcor.getConcorContributionId());
            standInContribution.setMaatId(currentMigrationEntity.getMaatId());
            return anonymisingDataService.anonymise(standInContribution);
        }
        //
        CONTRIBUTIONS contribution = contributionsMapperUtils.mapLineXMLToObject(currentConcor.getXmlContent());
        if (feature.outgoingAnonymized()) {
            log.info("Feature:OutgoingAnonymized: contribution data will be anonymized.");
            contribution = anonymisingDataService.anonymise(contribution);
        }
        return contribution;
    }
}
