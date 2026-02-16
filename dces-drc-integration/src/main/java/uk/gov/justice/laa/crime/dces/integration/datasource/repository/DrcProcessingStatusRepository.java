package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.DrcProcessingStatusEntity;

import java.time.Instant;

@Repository
public interface DrcProcessingStatusRepository extends JpaRepository<DrcProcessingStatusEntity, Long> {

    @Transactional
    long deleteByCreationTimestampBefore(Instant purgeBeforeDate);

    boolean existsByFdcIdAndDrcProcessingTimestampAndAckResponseStatus(
        long fdcId, Instant drcProcessingTimestamp, int ackResponseStatus);

    boolean existsByConcorContributionIdAndDrcProcessingTimestampAndAckResponseStatus(
        long concorContributionId, Instant drcProcessingTimestamp, int ackResponseStatus);
}