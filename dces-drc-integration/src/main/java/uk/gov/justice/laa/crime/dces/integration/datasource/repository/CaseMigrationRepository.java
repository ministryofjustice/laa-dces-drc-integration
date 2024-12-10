package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseMigrationEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseMigrationRepository extends JpaRepository<CaseMigrationEntity, Integer> {

    List<CaseMigrationEntity> getCaseMigrationEntitiesByBatchId(Long batchId);
    List<CaseMigrationEntity> getCaseMigrationEntitiesByBatchIdAndRecordTypeAndIsProcessed(Long batchId, String recordType, boolean isProcessed);

    @Transactional
    Long deleteByBatchId(Long batchId);

    @Query(value = "SELECT MAX(m.batch_id) FROM public.case_migration m", nativeQuery = true)
    Optional<Long> getHighestBatchNumber();

}
