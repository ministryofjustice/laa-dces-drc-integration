package uk.gov.justice.laa.crime.dces.integration.datasource;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestConfig;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
class DatabaseServiceTest extends ApplicationTestConfig {

    @Autowired
    private CaseSubmissionRepository caseSubmissionRepository;

    @BeforeEach
    public void setUp() {
        caseSubmissionRepository.deleteAll();
    }

    @Test
    void testDatabaseConnectionAndDataCaseRepo() {

        caseSubmissionRepository.save(createExpectedCaseSubmissionEntity(RecordType.CONTRIBUTION, 1, 200));

        long count = caseSubmissionRepository.count();

        assertEquals(1, count);
        assertEquals("Contribution", caseSubmissionRepository.findAll().stream().findFirst().get().getRecordType());
        assertEquals(200, caseSubmissionRepository.findAll().get(0).getHttpStatus());
    }

    private CaseSubmissionEntity createExpectedCaseSubmissionEntity(RecordType recordType, Integer eventTypeId, Integer httpStatusCode) {
        return CaseSubmissionEntity.builder()
                .fdcId(RecordType.FDC.equals(recordType) ? BigInteger.valueOf(-444) : null)
                .concorContributionId(RecordType.CONTRIBUTION.equals(recordType) ? BigInteger.valueOf(-333) : null)
                .recordType(recordType.getName())
                .eventType(eventTypeId)
                .httpStatus(httpStatusCode)
                .build();
    }
}