package uk.gov.justice.laa.crime.dces.integration.datasource;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.EventTypeRepository;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.math.BigInteger;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SoftAssertionsExtension.class)
class CaseSubmissionServiceTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Mock
    private CaseSubmissionRepository caseSubmissionRepository;
    @Mock
    private EventTypeRepository eventTypeRepository;

    @InjectMocks
    private CaseSubmissionService caseSubmissionService;
    @Captor
    private ArgumentCaptor<CaseSubmissionEntity> caseSubmissionEntityArgumentCaptor;

    private final BigInteger testTraceId = BigInteger.valueOf(-777);
    private final BigInteger testBatchId = BigInteger.valueOf(-666);
    private final BigInteger testMaatId = BigInteger.valueOf(-555);
    private final BigInteger testFdcId = BigInteger.valueOf(-444);
    private final BigInteger testConcorId = BigInteger.valueOf(-333);
    private final String testPayload = "TestPayload"+ LocalDateTime.now();

    @Test
    void whenLogFdcIsCalledWithAllDetails_thenLogEntryIsAsExpected() {
        Integer expectedHttpStatusCode = 200;
        EventType expectedEventType = EventType.SENT_TO_DRC;
        Fdc fdcObject = createTestFdcObject();

        when(caseSubmissionRepository.save(caseSubmissionEntityArgumentCaptor.capture())).thenReturn(new CaseSubmissionEntity());
        when(eventTypeRepository.getEventTypeEntityByDescriptionEquals(expectedEventType.getName())).thenReturn(new EventTypeEntity(1,expectedEventType.getName()));
        var expectedCaseSubmissionEntity = createExpectedCaseSubmissionEntity(RecordType.FDC, 1, expectedHttpStatusCode);

        caseSubmissionService.logFdcEvent(expectedEventType,testBatchId,testTraceId,fdcObject,expectedHttpStatusCode,testPayload);

        softly.assertThat(caseSubmissionEntityArgumentCaptor.getValue()).isEqualTo(expectedCaseSubmissionEntity);
        softly.assertAll();
    }

    @Test
    void whenLogFdcIsCalledWithNoEventType_thenLogEntryFails() {
        Integer expectedHttpStatusCode = 200;
        EventType expectedEventType = null;
        Fdc fdcObject = createTestFdcObject();

        try{
            caseSubmissionService.logFdcEvent(expectedEventType,testBatchId,testTraceId,fdcObject,expectedHttpStatusCode,testPayload);
            softly.fail("Exception for lack of eventType should be thrown");
        } catch (DcesDrcServiceException e){
            softly.assertThat("EventType cannot be null").isEqualTo(e.getMessage());
        } catch (Exception e){
            softly.fail("Unexpected Exception Thrown:"+ e.getMessage());
        }
        softly.assertAll();
    }

    @Test
    void whenLogContributionIsCalledWithAllDetails_thenLogEntryIsAsExpected() {
        Integer expectedHttpStatusCode = 200;
        EventType expectedEventType = EventType.SENT_TO_DRC;
        CONTRIBUTIONS contributionObject = createTestContributionObject();

        when(caseSubmissionRepository.save(caseSubmissionEntityArgumentCaptor.capture())).thenReturn(new CaseSubmissionEntity());
        when(eventTypeRepository.getEventTypeEntityByDescriptionEquals(expectedEventType.getName())).thenReturn(new EventTypeEntity(1,expectedEventType.getName()));
        var expectedCaseSubmissionEntity = createExpectedCaseSubmissionEntity(RecordType.CONTRIBUTION, 1, expectedHttpStatusCode);

        caseSubmissionService.logContributionEvent(testConcorId, expectedEventType,testBatchId,testTraceId,contributionObject,expectedHttpStatusCode,testPayload);

        softly.assertThat(caseSubmissionEntityArgumentCaptor.getValue()).isEqualTo(expectedCaseSubmissionEntity);
        softly.assertAll();
    }

    private CaseSubmissionEntity createExpectedCaseSubmissionEntity(RecordType recordType, Integer eventTypeId, Integer httpStatusCode){
        return CaseSubmissionEntity.builder()
                .id(null) // this will not be assigned till post-save.
                .traceId(testTraceId)
                .batchId(testBatchId)
                .maatId(testMaatId)
                .fdcId(RecordType.FDC.equals(recordType) ? testFdcId: null)
                .concorContributionId(RecordType.CONTRIBUTION.equals(recordType) ? testConcorId : null)
                .recordType(recordType.getName())
                .eventType(eventTypeId)
                .payload(testPayload)
                .httpStatus(httpStatusCode)
                .build();
    }

    private Fdc createTestFdcObject(){
        Fdc fdcObject = new Fdc();
        fdcObject.setMaatId(testMaatId);
        fdcObject.setId(testFdcId);
        return fdcObject;
    }

    private CONTRIBUTIONS createTestContributionObject(){
        CONTRIBUTIONS contributionObject = new CONTRIBUTIONS();
        contributionObject.setMaatId(testMaatId);
        contributionObject.setId(testConcorId);
        return contributionObject;
    }

}