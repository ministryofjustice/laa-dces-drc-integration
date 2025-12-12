package uk.gov.justice.laa.crime.dces.integration.datasource;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.justice.laa.crime.dces.integration.datasource.model.DrcProcessingStatusEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.DrcProcessingStatusRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.EventTypeRepository;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.time.Instant;
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

    public static final String SUCCESS_MESSAGE = "Success";

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Mock
    private CaseSubmissionRepository caseSubmissionRepository;
    @Mock
    private DrcProcessingStatusRepository drcProcessingStatusRepository;
    @Mock
    private EventTypeRepository eventTypeRepository;

    private EventService eventService;

    @Captor
    private ArgumentCaptor<CaseSubmissionEntity> caseSubmissionEntityArgumentCaptor;
    @Captor
    private ArgumentCaptor<DrcProcessingStatusEntity> drcProcessingStatusEntityArgumentCaptor;
    @Captor
    private ArgumentCaptor<Instant> instantArgumentCaptor;

    private final Long testTraceId = -777L;
    private final Long testBatchId = -666L;
    private final Long testMaatId = -555L;
    private final Long testFdcId = -444L;
    private final Long testConcorId = -333L;
    private final String testPayload = "TestPayload"+ LocalDateTime.now();

    @BeforeEach
    void setUp() {
        this.eventService = new EventService(caseSubmissionRepository, drcProcessingStatusRepository, eventTypeRepository);
    }

    @Test
    void whenSaveDrcProcessingStatusEntityWithValidEntity_thenDelegatesToRepositoryAndReturnsSavedEntity() {

        var input = DrcProcessingStatusEntity.builder()
                .maatId(testMaatId)
                .fdcId(testFdcId)
                .statusMessage(testPayload)
                .build();

        when(drcProcessingStatusRepository.save(any(DrcProcessingStatusEntity.class))).thenReturn(input);

        var result = eventService.saveDrcProcessingStatusEntity(input);

        verify(drcProcessingStatusRepository).save(drcProcessingStatusEntityArgumentCaptor.capture());
        softly.assertThat(drcProcessingStatusEntityArgumentCaptor.getValue()).isEqualTo(input);
        softly.assertThat(result).isEqualTo(input);
        softly.assertAll();
    }
    @Test
    void whenLogFdcErrorIsCalled_thenEntitySavedAndReturned() {
        var ack = mock(FdcAckFromDrc.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn(SUCCESS_MESSAGE);
        when(pd.getDetail()).thenReturn("C Detail");
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().fdcId()).thenReturn(testFdcId);
        when(ack.data().maatId()).thenReturn(testMaatId);
        when(ack.data().errorText()).thenReturn("ignored");

        when(drcProcessingStatusRepository.save(any(DrcProcessingStatusEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = eventService.logFdcError(ack);

        verify(drcProcessingStatusRepository).save(drcProcessingStatusEntityArgumentCaptor.capture());
        var captured = drcProcessingStatusEntityArgumentCaptor.getValue();

        softly.assertThat(result).isEqualTo(captured);
        softly.assertThat(captured.getMaatId()).isEqualTo(testMaatId);
        softly.assertThat(captured.getFdcId()).isEqualTo(testFdcId);
        softly.assertThat(captured.getStatusMessage()).isEqualTo(SUCCESS_MESSAGE);
        softly.assertThat(captured.getDetail()).isEqualTo("C Detail");
        softly.assertAll();
    }
    @Test
    void whenLogFdcErrorRepositoryThrows_thenReturnsNull() {
        var ack = mock(FdcAckFromDrc.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn(SUCCESS_MESSAGE);
        when(pd.getDetail()).thenReturn("C Detail");
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().fdcId()).thenReturn(testFdcId);
        when(ack.data().maatId()).thenReturn(testMaatId);
        when(ack.data().errorText()).thenReturn("ignored");

        when(drcProcessingStatusRepository.save(any(DrcProcessingStatusEntity.class)))
                .thenThrow(new RuntimeException("DB down"));

        var result = eventService.logFdcError(ack);

        verify(drcProcessingStatusRepository).save(drcProcessingStatusEntityArgumentCaptor.capture());
        var captured = drcProcessingStatusEntityArgumentCaptor.getValue();

        softly.assertThat(result).isNull();
        softly.assertThat(captured.getMaatId()).isEqualTo(testMaatId);
        softly.assertThat(captured.getFdcId()).isEqualTo(testFdcId);
        softly.assertThat(captured.getStatusMessage()).isEqualTo(SUCCESS_MESSAGE);
        softly.assertThat(captured.getDetail()).isEqualTo("C Detail");
        softly.assertAll();
    }

    @Test
    void whenLogConcorContributionErrorIsCalled_thenEntitySavedAndReturned() {
        var ack = mock(ConcorContributionAckFromDrc.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn(SUCCESS_MESSAGE);
        when(pd.getDetail()).thenReturn("C Detail");
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().concorContributionId()).thenReturn(testConcorId);
        when(ack.data().maatId()).thenReturn(testMaatId);
        when(ack.data().errorText()).thenReturn("ignored");

        when(drcProcessingStatusRepository.save(any(DrcProcessingStatusEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = eventService.logConcorContributionError(ack);

        verify(drcProcessingStatusRepository).save(drcProcessingStatusEntityArgumentCaptor.capture());
        var captured = drcProcessingStatusEntityArgumentCaptor.getValue();

        softly.assertThat(result).isEqualTo(captured);
        softly.assertThat(captured.getMaatId()).isEqualTo(testMaatId);
        softly.assertThat(captured.getConcorContributionId()).isEqualTo(testConcorId);
        softly.assertThat(captured.getStatusMessage()).isEqualTo(SUCCESS_MESSAGE);
        softly.assertThat(captured.getDetail()).isEqualTo("C Detail");
        softly.assertAll();
    }

    @Test
    void whenLogConcorContributionErrorRepositoryThrows_thenReturnsNull() {
        var ack = mock(ConcorContributionAckFromDrc.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn(SUCCESS_MESSAGE);
        when(pd.getDetail()).thenReturn("C Detail");
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().concorContributionId()).thenReturn(testConcorId);
        when(ack.data().maatId()).thenReturn(testMaatId);
        when(ack.data().errorText()).thenReturn("ignored");

        when(drcProcessingStatusRepository.save(any(DrcProcessingStatusEntity.class)))
                .thenThrow(new RuntimeException("DB down"));

        var result = eventService.logConcorContributionError(ack);

        verify(drcProcessingStatusRepository).save(drcProcessingStatusEntityArgumentCaptor.capture());
        var captured = drcProcessingStatusEntityArgumentCaptor.getValue();

        softly.assertThat(result).isNull();
        softly.assertThat(captured.getMaatId()).isEqualTo(testMaatId);
        softly.assertThat(captured.getConcorContributionId()).isEqualTo(testConcorId);
        softly.assertThat(captured.getStatusMessage()).isEqualTo(SUCCESS_MESSAGE);
        softly.assertThat(captured.getDetail()).isEqualTo("C Detail");
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
    void givenAValidCronExpression_whenPurgeDrcProcessingStatusEntriesIsInvoked_shouldPurgePeriodicRecords() {
        Instant startOfTest = Instant.now();
        eventService.setHistoryCutoffMonth(1);
        when(drcProcessingStatusRepository.deleteByCreationTimestampBefore(any(Instant.class))).thenReturn(5l);
        softly.assertThat(eventService.purgePeriodicDrcProcessingStatusEntries()).isEqualTo(5l);
        verify(drcProcessingStatusRepository).deleteByCreationTimestampBefore(instantArgumentCaptor.capture());
        softly.assertThat(instantArgumentCaptor.getValue()).isBefore(startOfTest);
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