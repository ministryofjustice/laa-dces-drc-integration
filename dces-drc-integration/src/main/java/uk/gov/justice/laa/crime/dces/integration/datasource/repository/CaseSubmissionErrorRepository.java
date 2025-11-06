package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionErrorEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface CaseSubmissionErrorRepository extends JpaRepository<CaseSubmissionErrorEntity, Long> {

    @Transactional
    long deleteByCreationDateBefore(LocalDateTime purgeBeforeDate);
}