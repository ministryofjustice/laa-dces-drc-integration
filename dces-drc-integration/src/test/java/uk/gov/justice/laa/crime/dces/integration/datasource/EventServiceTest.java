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
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.EventTypeRepository;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    private EventTypeRepository eventTypeRepository;

    @InjectMocks
    private EventService eventService;
    @Captor
    private ArgumentCaptor<CaseSubmissionEntity> caseSubmissionEntityArgumentCaptor;

    private final Long testTraceId = -777L;
    private final Long testBatchId = -666L;
    private final Long testMaatId = -555L;
    private final Long testFdcId = -444L;
    private final Long testConcorId = -333L;
    private final String testPayload = "TestPayload"+ LocalDateTime.now();

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
    void whenHistoryCutoffIsSet_thenReturnsIntOnDeletion() throws NoSuchFieldException, IllegalAccessException {
        setCutoff("5");
        LocalDateTime cutoffDate = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(1)
                .minusDays(5);
        when(caseSubmissionRepository.deleteByProcessedDateBefore(cutoffDate)).thenReturn(300);
        softly.assertThat(eventService.getCutoffDays()).isEqualTo(5);
        softly.assertThat(eventService.deleteHistoricalCaseSubmissionEntries()).isEqualTo(300);
        verify(caseSubmissionRepository, times(1)).deleteByProcessedDateBefore(cutoffDate);
    }

    @Test
    void whenHistoryCutoffIsSet_thenReturnsIntOnCount() throws NoSuchFieldException, IllegalAccessException {
        setCutoff("5");
        LocalDateTime cutoffDate = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(1)
                .minusDays(5);
        when(caseSubmissionRepository.countByProcessedDateBefore(cutoffDate)).thenReturn(300L);
        softly.assertThat(eventService.getCutoffDays()).isEqualTo(5);
        softly.assertThat(eventService.countHistoricalCaseSubmissionEntries()).isEqualTo(300L);
        verify(caseSubmissionRepository, times(1)).countByProcessedDateBefore(cutoffDate);
    }

    @Test
    void whenNoHistoryCutoffIsSet_thenZeroReturnedOnDeletion() throws NoSuchFieldException, IllegalAccessException {
        setCutoff(null);
        softly.assertThat(eventService.getCutoffDays()).isEqualTo(null);
        softly.assertThat(eventService.deleteHistoricalCaseSubmissionEntries()).isEqualTo(0);
        verify(caseSubmissionRepository, never()).deleteByProcessedDateBefore(any());
    }

    @Test
    void whenNoHistoryCutoffIsSet_thenZeroReturnedOnCount() throws NoSuchFieldException, IllegalAccessException {
        setCutoff(null);
        softly.assertThat(eventService.getCutoffDays()).isEqualTo(null);
        softly.assertThat(eventService.countHistoricalCaseSubmissionEntries()).isEqualTo(0);
        verify(caseSubmissionRepository, never()).countByProcessedDateBefore(any());
    }

    @Test
    void whenHistoryCutoffInvalid_thenZeroReturnedOnDeletion() throws NoSuchFieldException, IllegalAccessException {
        setCutoff("Not Numbers");
        softly.assertThat(eventService.getCutoffDays()).isEqualTo(null);
        softly.assertThat(eventService.deleteHistoricalCaseSubmissionEntries()).isEqualTo(0);
        verify(caseSubmissionRepository, never()).deleteByProcessedDateBefore(any());
    }

    @Test
    void whenHistoryCutoffInvalid_thenZeroReturnedOnCount() throws NoSuchFieldException, IllegalAccessException {
        setCutoff("Not Numbers");
        softly.assertThat(eventService.getCutoffDays()).isEqualTo(null);
        softly.assertThat(eventService.countHistoricalCaseSubmissionEntries()).isEqualTo(0);
        verify(caseSubmissionRepository, never()).countByProcessedDateBefore(any());
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

    private void setCutoff(String cutoff) throws NoSuchFieldException, IllegalAccessException {
        Field privateStringField = EventService.class.getDeclaredField("HISTORY_CUTOFF_DAYS");
        privateStringField.setAccessible(true);
        privateStringField.set(eventService, cutoff);
    }

}