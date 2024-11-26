package uk.gov.justice.laa.crime.dces.integration.testing;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DbLoggingTest {

  @Autowired
  private EventService eventService;
  @Autowired
  private CaseSubmissionRepository caseSubmissionRepository;

  private final String testPayloadString = "TestData"+ LocalDateTime.now();
  private static final Long TEST_BATCH_ID = -999L;
  private static final Long TEST_TRACE_ID = -999L;

  // Method to clear out any lingering test data that might exist.
  @AfterAll
  @BeforeAll
  public void deleteTestData(){
    caseSubmissionRepository.deleteByBatchIdAndTraceId(TEST_BATCH_ID, TEST_TRACE_ID);
  }

  @Test
  void given_FdcAllRequiredValues_thenSaves(){
    var fdc = createTestFdc();
    boolean response = eventService.logFdc(EventType.SENT_TO_DRC, TEST_BATCH_ID, TEST_TRACE_ID, fdc, HttpStatus.OK, testPayloadString );
    assertTrue(response);
    clearDownData(1L);
  }

  @Test
  void given_FdcMinimalValues_thenSaves(){
    boolean response = eventService.logFdc(EventType.SENT_TO_DRC, TEST_BATCH_ID, TEST_TRACE_ID, null, null, testPayloadString );
    assertTrue(response);
    clearDownData(1L);
  }

  @Test
  void given_ContributionAllRequiredValues_thenSaves(){
    var contribution = createTestContribution();
    boolean response = eventService.logConcor(-8888L, EventType.SENT_TO_DRC, TEST_BATCH_ID, TEST_TRACE_ID, contribution, HttpStatus.OK, testPayloadString );
    assertTrue(response);
    clearDownData(1L);
  }

  @Test
  void given_MissingTypeValue_thenError(){
    var contribution = createTestContribution();
    assertThrows(DcesDrcServiceException.class,() -> eventService.logConcor(-8888L, null, TEST_BATCH_ID, TEST_TRACE_ID, contribution, HttpStatus.OK, testPayloadString ));
    clearDownData(0L);
  }

  @Test
  void given_MissingContributionValue_thenSave(){
    boolean response = eventService.logConcor(-8888L, EventType.SENT_TO_DRC, TEST_BATCH_ID, TEST_TRACE_ID, null, HttpStatus.OK, testPayloadString );
    assertTrue(response);
    clearDownData(1L);
  }

  private void clearDownData(Long expectedDeletions){
    Long deletions = caseSubmissionRepository.deleteByPayloadAndBatchIdAndTraceId(testPayloadString, TEST_BATCH_ID, TEST_TRACE_ID);
    assertEquals(expectedDeletions, deletions);
  }

  private CONTRIBUTIONS createTestContribution(){
    var contribution = new CONTRIBUTIONS();
    contribution.setId(1000L);
    contribution.setMaatId(2000L);
    return contribution;
  }

  private Fdc createTestFdc(){
    var fdc = new Fdc();
    fdc.setId(1000L);
    fdc.setMaatId(2000L);
    return fdc;
  }


}