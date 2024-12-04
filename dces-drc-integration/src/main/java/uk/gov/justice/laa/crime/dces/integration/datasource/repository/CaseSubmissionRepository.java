package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;

import java.util.List;

@Repository
public interface CaseSubmissionRepository extends JpaRepository<CaseSubmissionEntity, Integer> {

    @Query(value = "select nextval('trace_id_seq')", nativeQuery = true)
    Long getNextTraceId();

    @Query(value = "select nextval('batch_id_seq')", nativeQuery = true)
    Long getNextBatchId();

    @Transactional
    Long deleteByPayloadAndBatchIdAndTraceId(String payload, Long batchId, Long traceId);

    @Transactional
    Long deleteByBatchIdAndTraceId(Long batchId, Long traceId);

    List<CaseSubmissionEntity> findAllByBatchId(Long batchId);

    @Transactional
    Long deleteAllByBatchId(Long batchId);

}
