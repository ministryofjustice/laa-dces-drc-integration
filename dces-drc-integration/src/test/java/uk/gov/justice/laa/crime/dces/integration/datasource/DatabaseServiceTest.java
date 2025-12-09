package uk.gov.justice.laa.crime.dces.integration.datasource;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseMigrationEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.DrcProcessingStatusEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseMigrationRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.DrcProcessingStatusRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
class DatabaseServiceTest extends ApplicationTestBase {

    @Autowired
    private CaseSubmissionRepository caseSubmissionRepository;
    @Autowired
    private DrcProcessingStatusRepository drcProcessingStatusRepository;
    @Autowired
    private CaseMigrationRepository caseMigrationRepository;

    @BeforeEach
    public void setUp() {
        caseSubmissionRepository.deleteAll();
        drcProcessingStatusRepository.deleteAll();
    }

    @Test
    void testDatabaseConnectionAndDataCaseRepo() {

        caseSubmissionRepository.save(createExpectedCaseSubmissionEntity(RecordType.CONTRIBUTION, 1, 200));

        long count = caseSubmissionRepository.count();

        assertEquals(1, count);
        assertEquals("Contribution", caseSubmissionRepository.findAll().stream().findFirst().get().getRecordType());
        assertEquals(200, caseSubmissionRepository.findAll().get(0).getHttpStatus());
    }

    @Test
    void testDatabaseConnectionAndContributionDataDrcProcessingStatus() {

        drcProcessingStatusRepository.save(createExpectedDrcProcessingStatusEntity(RecordType.CONTRIBUTION, 200));

        long count = drcProcessingStatusRepository.count();

        assertEquals(1, count);
        assertEquals(-333L, drcProcessingStatusRepository.findAll().stream().findFirst().get().getConcorContributionId());
    }

    @Test
    void testDatabaseConnectionAndFdcDataDrcProcessingStatus() {

        drcProcessingStatusRepository.save(createExpectedDrcProcessingStatusEntity(RecordType.FDC, 200));

        long count = drcProcessingStatusRepository.count();

        assertEquals(1, count);
        assertEquals(-333L, drcProcessingStatusRepository.findAll().stream().findFirst().get().getFdcId());
    }

    @Test
    void givenAValidCaseSubmission_whenDeleteByCreationDateBeforeIsInvoked_shouldDeleteOldMessage() {

        DrcProcessingStatusEntity entity = createExpectedDrcProcessingStatusEntity(RecordType.FDC, 200);
        drcProcessingStatusRepository.save(entity);
        long count = drcProcessingStatusRepository.count();
        assertEquals(1, count);
        long deleteCount =  drcProcessingStatusRepository.deleteByCreationDateBefore(LocalDateTime.now());
        assertEquals(1, deleteCount);
    }

    @Test
    void testCaseMigrationCreation(){
        CaseMigrationEntity entity = CaseMigrationEntity.builder()
                .batchId(-111L)
                .recordType(RecordType.CONTRIBUTION.getName())
                .concorContributionId(-555L)
                .fdcId(-222L)
                .maatId(-9999L)
                .payload("TestPayload")
                .build();
        caseMigrationRepository.save(entity);
        assertEquals(1,caseMigrationRepository.findAll().size());
    }

    private CaseSubmissionEntity createExpectedCaseSubmissionEntity(RecordType recordType, Integer eventTypeId, Integer httpStatusCode) {
        return CaseSubmissionEntity.builder()
                .fdcId(RecordType.FDC.equals(recordType) ? -444L : null)
                .concorContributionId(RecordType.CONTRIBUTION.equals(recordType) ? -333L : null)
                .recordType(recordType.getName())
                .eventType(eventTypeId)
                .httpStatus(httpStatusCode)
                .build();
    }

    private DrcProcessingStatusEntity createExpectedDrcProcessingStatusEntity(RecordType recordType, Integer httpStatusCode) {
        return DrcProcessingStatusEntity.builder()
                .fdcId(RecordType.FDC.equals(recordType) ? -333L : null)
                .concorContributionId(RecordType.CONTRIBUTION.equals(recordType) ? -333L : null)
                .statusMessage("MAAT ID Missing")
                .detail("MAAT ID is required for processing")
                .build();
    }
}