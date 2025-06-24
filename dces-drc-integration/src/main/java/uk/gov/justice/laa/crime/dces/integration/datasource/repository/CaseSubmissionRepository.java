package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaseSubmissionRepository extends JpaRepository<CaseSubmissionEntity, Integer> {

    @Query(value = "select nextval('trace_id_seq')", nativeQuery = true)
    Long getNextTraceId();

    @Query(value = "select nextval('batch_id_seq')", nativeQuery = true)
    Long getNextBatchId();

    List<CaseSubmissionEntity> findAllByBatchId(Long batchId);

    Long countByProcessedDateBefore(LocalDateTime processedDate);

    @Transactional
    Long deleteByPayloadAndBatchIdAndTraceId(String payload, Long batchId, Long traceId);

    @Transactional
    Long deleteByBatchIdAndTraceId(Long batchId, Long traceId);

    @Transactional
    Long deleteAllByBatchId(Long batchId);

    @Transactional
    @Modifying
    @Query(value = """
            delete from public.case_submission
            where processed_date < :cutoffDate
            """, nativeQuery = true)
    Integer deleteByProcessedDateBefore(@Param("cutoffDate") LocalDateTime cutoffDate);


}
