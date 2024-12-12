package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseMigrationEntity;

import java.util.List;

@Repository
public interface CaseMigrationRepository extends JpaRepository<CaseMigrationEntity, Integer> {

    List<CaseMigrationEntity> getCaseMigrationEntitiesByBatchId(Long batchId);
    List<CaseMigrationEntity> getCaseMigrationEntitiesByBatchIdAndRecordType(Long batchId, String recordType);

    @Transactional
    Long deleteByBatchId(Long batchId);


}
