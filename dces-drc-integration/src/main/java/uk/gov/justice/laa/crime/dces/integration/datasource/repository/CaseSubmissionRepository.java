package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;

import java.math.BigInteger;

public interface CaseSubmissionRepository extends JpaRepository<CaseSubmissionEntity, Integer> {

    @Query(value = "select nextval('trace_id_seq')", nativeQuery = true)
    BigInteger getNextTraceId();

    @Query(value = "select nextval('batch_id_seq')", nativeQuery = true)
    BigInteger getNextBatchId();

    @Transactional
    Long deleteByPayloadAndBatchIdAndTraceId(String payload, BigInteger batchId, BigInteger traceId);

}
