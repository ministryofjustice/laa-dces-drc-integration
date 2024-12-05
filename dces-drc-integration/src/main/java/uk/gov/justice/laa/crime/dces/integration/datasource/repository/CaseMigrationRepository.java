package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseMigrationEntity;

@Repository
public interface CaseMigrationRepository extends JpaRepository<CaseMigrationEntity, Integer> {

    CaseMigrationEntity getCaseMigrationEntitiesByBatchId(Long batchId);

}
