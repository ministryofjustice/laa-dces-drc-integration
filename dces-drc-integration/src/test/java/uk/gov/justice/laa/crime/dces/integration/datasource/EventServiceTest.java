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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionErrorEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionErrorRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.EventTypeRepository;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SoftAssertionsExtension.class)
class EventServiceTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Mock
    private CaseSubmissionRepository caseSubmissionRepository;
    @Mock
    private CaseSubmissionErrorRepository caseSubmissionErrorRepository;
    @Mock
    private EventTypeRepository eventTypeRepository;

    @InjectMocks
    private EventService eventService;
    @Captor
    private ArgumentCaptor<CaseSubmissionEntity> caseSubmissionEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<CaseSubmissionErrorEntity> caseSubmissionErrorEntityArgumentCaptor;

    private final Long testTraceId = -777L;
    private final Long testBatchId = -666L;
    private final Long testMaatId = -555L;
    private final Long testFdcId = -444L;
    private final Long testConcorId = -333L;
    private final String testPayload = "TestPayload"+ LocalDateTime.now();


    @Test
    void whenSaveCaseSubmissionErrorEntityWithValidEntity_thenDelegatesToRepositoryAndReturnsSavedEntity() {

        var input = CaseSubmissionErrorEntity.builder()
                .maatId(testMaatId)
                .fdcId(testFdcId)
                .title(testPayload)
                .build();

        when(caseSubmissionErrorRepository.save(any(CaseSubmissionErrorEntity.class))).thenReturn(input);

        var result = eventService.saveCaseSubmissionErrorEntity(input);

        verify(caseSubmissionErrorRepository).save(caseSubmissionErrorEntityArgumentCaptor.capture());
        softly.assertThat(caseSubmissionErrorEntityArgumentCaptor.getValue()).isEqualTo(input);
        softly.assertThat(result).isEqualTo(input);
        softly.assertAll();
    }
    @Test
    void whenLogFdcErrorIsCalled_thenEntitySavedAndReturned() {
        var ack = mock(FdcAckFromDrc.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn("C Title");
        when(pd.getDetail()).thenReturn("C Detail");
        when(pd.getStatus()).thenReturn(500);
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().fdcId()).thenReturn(testFdcId);
        when(ack.data().maatId()).thenReturn(testMaatId);
        when(ack.data().errorText()).thenReturn("ignored");

        when(caseSubmissionErrorRepository.save(any(CaseSubmissionErrorEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = eventService.logFdcError(ack);

        verify(caseSubmissionErrorRepository).save(caseSubmissionErrorEntityArgumentCaptor.capture());
        var captured = caseSubmissionErrorEntityArgumentCaptor.getValue();

        softly.assertThat(result).isEqualTo(captured);
        softly.assertThat(captured.getMaatId()).isEqualTo(testMaatId);
        softly.assertThat(captured.getFdcId()).isEqualTo(testFdcId);
        softly.assertThat(captured.getTitle()).isEqualTo("C Title");
        softly.assertThat(captured.getDetail()).isEqualTo("C Detail");
        softly.assertThat(captured.getStatus()).isEqualTo(Integer.valueOf(500));
        softly.assertAll();
    }
    @Test
    void whenLogFdcErrorRepositoryThrows_thenReturnsNull() {
        var ack = mock(FdcAckFromDrc.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn("C Title");
        when(pd.getDetail()).thenReturn("C Detail");
        when(pd.getStatus()).thenReturn(500);
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().fdcId()).thenReturn(testFdcId);
        when(ack.data().maatId()).thenReturn(testMaatId);
        when(ack.data().errorText()).thenReturn("ignored");

        when(caseSubmissionErrorRepository.save(any(CaseSubmissionErrorEntity.class)))
                .thenThrow(new RuntimeException("DB down"));

        var result = eventService.logFdcError(ack);

        verify(caseSubmissionErrorRepository).save(caseSubmissionErrorEntityArgumentCaptor.capture());
        var captured = caseSubmissionErrorEntityArgumentCaptor.getValue();

        softly.assertThat(result).isNull();
        softly.assertThat(captured.getMaatId()).isEqualTo(testMaatId);
        softly.assertThat(captured.getFdcId()).isEqualTo(testFdcId);
        softly.assertThat(captured.getTitle()).isEqualTo("C Title");
        softly.assertThat(captured.getDetail()).isEqualTo("C Detail");
        softly.assertThat(captured.getStatus()).isEqualTo(Integer.valueOf(500));
        softly.assertAll();
    }

    @Test
    void whenLogConcorContributionErrorIsCalled_thenEntitySavedAndReturned() {
        var ack = mock(ConcorContributionAckFromDrc.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn("C Title");
        when(pd.getDetail()).thenReturn("C Detail");
        when(pd.getStatus()).thenReturn(500);
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().concorContributionId()).thenReturn(testConcorId);
        when(ack.data().maatId()).thenReturn(testMaatId);
        when(ack.data().errorText()).thenReturn("ignored");

        when(caseSubmissionErrorRepository.save(any(CaseSubmissionErrorEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = eventService.logConcorContributionError(ack);

        verify(caseSubmissionErrorRepository).save(caseSubmissionErrorEntityArgumentCaptor.capture());
        var captured = caseSubmissionErrorEntityArgumentCaptor.getValue();

        softly.assertThat(result).isEqualTo(captured);
        softly.assertThat(captured.getMaatId()).isEqualTo(testMaatId);
        softly.assertThat(captured.getConcorContributionId()).isEqualTo(testConcorId);
        softly.assertThat(captured.getTitle()).isEqualTo("C Title");
        softly.assertThat(captured.getDetail()).isEqualTo("C Detail");
        softly.assertThat(captured.getStatus()).isEqualTo(Integer.valueOf(500));
        softly.assertAll();
    }

    @Test
    void whenLogConcorContributionErrorRepositoryThrows_thenReturnsNull() {
        var ack = mock(ConcorContributionAckFromDrc.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn("C Title");
        when(pd.getDetail()).thenReturn("C Detail");
        when(pd.getStatus()).thenReturn(500);
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().concorContributionId()).thenReturn(testConcorId);
        when(ack.data().maatId()).thenReturn(testMaatId);
        when(ack.data().errorText()).thenReturn("ignored");

        when(caseSubmissionErrorRepository.save(any(CaseSubmissionErrorEntity.class)))
                .thenThrow(new RuntimeException("DB down"));

        var result = eventService.logConcorContributionError(ack);

        verify(caseSubmissionErrorRepository).save(caseSubmissionErrorEntityArgumentCaptor.capture());
        var captured = caseSubmissionErrorEntityArgumentCaptor.getValue();

        softly.assertThat(result).isNull();
        softly.assertThat(captured.getMaatId()).isEqualTo(testMaatId);
        softly.assertThat(captured.getConcorContributionId()).isEqualTo(testConcorId);
        softly.assertThat(captured.getTitle()).isEqualTo("C Title");
        softly.assertThat(captured.getDetail()).isEqualTo("C Detail");
        softly.assertThat(captured.getStatus()).isEqualTo(Integer.valueOf(500));
        softly.assertAll();
    }


    @Test
    void whenLogFdcIsCalledWithAllDetails_thenLogEntryIsAsExpected() {
        HttpStatusCode expectedHttpStatusCode = HttpStatus.OK;
        EventType expectedEventType = EventType.SENT_TO_DRC;
        Fdc fdcObject = createTestFdcObject();

        when(caseSubmissionRepository.save(caseSubmissionEntityArgumentCaptor.capture())).thenReturn(new CaseSubmissionEntity());
        when(eventTypeRepository.getEventTypeEntityByDescriptionEquals(expectedEventType.getName())).thenReturn(new EventTypeEntity(1,expectedEventType.getName()));
        var expectedCaseSubmissionEntity = createExpectedCaseSubmissionEntity(RecordType.FDC, 1, testTraceId, expectedHttpStatusCode);

        eventService.logFdc(expectedEventType,testBatchId,testTraceId,fdcObject,expectedHttpStatusCode,testPayload);

        softly.assertThat(caseSubmissionEntityArgumentCaptor.getValue()).isEqualTo(expectedCaseSubmissionEntity);
        softly.assertAll();
    }

    @Test
    void whenLogFdcIsCalledWithNoTrace_thenLogEntryIsAsExpected() {
        HttpStatusCode expectedHttpStatusCode = HttpStatus.OK;
        EventType expectedEventType = EventType.SENT_TO_DRC;
        Fdc fdcObject = createTestFdcObject();

        when(caseSubmissionRepository.save(caseSubmissionEntityArgumentCaptor.capture())).thenReturn(new CaseSubmissionEntity());
        when(eventTypeRepository.getEventTypeEntityByDescriptionEquals(expectedEventType.getName())).thenReturn(new EventTypeEntity(1,expectedEventType.getName()));
        var expectedCaseSubmissionEntity = createExpectedCaseSubmissionEntity(RecordType.FDC, 1, null, expectedHttpStatusCode);

        eventService.logFdc(expectedEventType,testBatchId,fdcObject,expectedHttpStatusCode,testPayload);

        softly.assertThat(caseSubmissionEntityArgumentCaptor.getValue()).isEqualTo(expectedCaseSubmissionEntity);
        softly.assertAll();
    }

    @Test
    void whenLogFdcIsCalledWithNoEventType_thenLogEntryFails() {
        HttpStatusCode expectedHttpStatusCode = HttpStatus.OK;
        EventType expectedEventType = null;
        Fdc fdcObject = createTestFdcObject();

        try{
            eventService.logFdc(expectedEventType,testBatchId,testTraceId,fdcObject,expectedHttpStatusCode,testPayload);
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
        HttpStatusCode expectedHttpStatusCode = HttpStatus.OK;
        EventType expectedEventType = EventType.SENT_TO_DRC;
        CONTRIBUTIONS contributionObject = createTestContributionObject();

        when(caseSubmissionRepository.save(caseSubmissionEntityArgumentCaptor.capture())).thenReturn(new CaseSubmissionEntity());
        when(eventTypeRepository.getEventTypeEntityByDescriptionEquals(expectedEventType.getName())).thenReturn(new EventTypeEntity(1,expectedEventType.getName()));
        var expectedCaseSubmissionEntity = createExpectedCaseSubmissionEntity(RecordType.CONTRIBUTION, 1, testTraceId, expectedHttpStatusCode);

        eventService.logConcor(testConcorId, expectedEventType,testBatchId,testTraceId,contributionObject,expectedHttpStatusCode,testPayload);

        softly.assertThat(caseSubmissionEntityArgumentCaptor.getValue()).isEqualTo(expectedCaseSubmissionEntity);
        softly.assertAll();
    }

    @Test
    void whenLogContributionIsCalledWithNoTraceId_thenLogEntryIsAsExpected() {
        HttpStatusCode expectedHttpStatusCode = HttpStatus.OK;
        EventType expectedEventType = EventType.SENT_TO_DRC;
        CONTRIBUTIONS contributionObject = createTestContributionObject();

        when(caseSubmissionRepository.save(caseSubmissionEntityArgumentCaptor.capture())).thenReturn(new CaseSubmissionEntity());
        when(eventTypeRepository.getEventTypeEntityByDescriptionEquals(expectedEventType.getName())).thenReturn(new EventTypeEntity(1,expectedEventType.getName()));
        var expectedCaseSubmissionEntity = createExpectedCaseSubmissionEntity(RecordType.CONTRIBUTION, 1, null, expectedHttpStatusCode);

        eventService.logConcor(testConcorId, expectedEventType,testBatchId,contributionObject,expectedHttpStatusCode,testPayload);

        softly.assertThat(caseSubmissionEntityArgumentCaptor.getValue()).isEqualTo(expectedCaseSubmissionEntity);
        softly.assertAll();
    }

    @Test
    void whenGenerateBatchIdIsCalled_thenBatchIdIsReturned(){
        when(caseSubmissionRepository.getNextBatchId()).thenReturn(testBatchId);
        Long actualBatchId = eventService.generateBatchId();
        softly.assertThat(actualBatchId).isEqualTo(testBatchId);
        softly.assertAll();
    }
    @Test
    void whenGenerateTraceIdIsCalled_thenBatchIdIsReturned(){
        when(caseSubmissionRepository.getNextTraceId()).thenReturn(testTraceId);
        Long actualTraceId = eventService.generateTraceId();
        softly.assertThat(actualTraceId).isEqualTo(testTraceId);
        softly.assertAll();
    }

    @Test
    void givenAValidCronExpression_whenPurgePeriodicCaseSubmissionEntriesIsInvoked_shouldPurgePeriodicRecords() {
        when(caseSubmissionRepository.deleteByProcessedDateBefore(any(LocalDateTime.class))).thenReturn(300);
        softly.assertThat(eventService.purgePeriodicCaseSubmissionEntries()).isEqualTo(300);
        verify(caseSubmissionRepository, times(1)).deleteByProcessedDateBefore(any(LocalDateTime.class));
        softly.assertAll();
    }

    @Test
    void givenAValidCronExpression_whenCountHistoricalCaseSubmissionEntriesIsInvoked_thenShouldReturnCount() {
        when(caseSubmissionRepository.countByProcessedDateBefore(any(LocalDateTime.class))).thenReturn(300L);
        softly.assertThat(eventService.countHistoricalCaseSubmissionEntries()).isEqualTo(300L);
        verify(caseSubmissionRepository, times(1)).countByProcessedDateBefore(any(LocalDateTime.class));
        softly.assertAll();
    }

    @Test
    void givenAValidCronExpression_whenPurgeCaseSubmissionErrorEntriesIsInvoked_shouldPurgePeriodicRecords() {
        when(caseSubmissionErrorRepository.deleteByCreationDateBefore(any(LocalDateTime.class))).thenReturn(5l);
        softly.assertThat(eventService.purgePeriodicCaseSubmissionErrorEntries()).isEqualTo(5l);
        verify(caseSubmissionErrorRepository).deleteByCreationDateBefore(any(LocalDateTime.class));
        softly.assertAll();
    }

    private CaseSubmissionEntity createExpectedCaseSubmissionEntity(RecordType recordType, Integer eventTypeId, Long traceId,HttpStatusCode httpStatusCode){
        return CaseSubmissionEntity.builder()
                .id(null) // this will not be assigned till post-save.
                .traceId(traceId)
                .batchId(testBatchId)
                .maatId(testMaatId)
                .fdcId(RecordType.FDC.equals(recordType) ? testFdcId: null)
                .concorContributionId(RecordType.CONTRIBUTION.equals(recordType) ? testConcorId : null)
                .recordType(recordType.getName())
                .eventType(eventTypeId)
                .payload(testPayload)
                .httpStatus( Objects.nonNull(httpStatusCode) ? httpStatusCode.value() : null )
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