package uk.gov.justice.laa.crime.dces.integration.testing;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.datasource.CaseSubmissionService;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.math.BigInteger;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DbLoggingTest {

  @Autowired
  private CaseSubmissionService caseSubmissionService;
  @Autowired
  private CaseSubmissionRepository caseSubmissionRepository;

  private final String testPayloadString = "TestData"+ LocalDateTime.now();
  private final BigInteger testBatchId = BigInteger.valueOf(-999);
  private final BigInteger testTraceId = BigInteger.valueOf(-999);

  // Method to clear out any lingering test data that might exist.
  @AfterAll
  @BeforeAll
  public void deleteTestData(){
    caseSubmissionRepository.deleteByBatchIdAndTraceId(testBatchId, testTraceId);
  }

  @Test
  void given_FdcAllRequiredValues_thenSaves(){
    var fdc = createTestFdc();
    boolean response = caseSubmissionService.logFdcEvent(EventType.SENT_TO_DRC, testBatchId,testTraceId, fdc, 200, testPayloadString );
    assertTrue(response);
    clearDownData(1L);
  }

  @Test
  void given_FdcMinimalValues_thenSaves(){
    boolean response = caseSubmissionService.logFdcEvent(EventType.SENT_TO_DRC, testBatchId,testTraceId, null, null, testPayloadString );
    assertTrue(response);
    clearDownData(1L);
  }

  @Test
  void given_ContributionAllRequiredValues_thenSaves(){
    var contribution = createTestContribution();
    boolean response = caseSubmissionService.logContributionCall(EventType.SENT_TO_DRC, testBatchId,testTraceId, contribution, 200, testPayloadString );
    assertTrue(response);
    clearDownData(1L);
  }

  @Test
  void given_MissingTypeValue_thenError(){
    var contribution = createTestContribution();
    assertThrows(DcesDrcServiceException.class,() -> caseSubmissionService.logContributionCall(null, testBatchId,testTraceId, contribution, 200, testPayloadString ));
    clearDownData(0L);
  }

  @Test
  void given_MissingContributionValue_thenSave(){
    boolean response = caseSubmissionService.logContributionCall(EventType.SENT_TO_DRC, testBatchId,testTraceId, null, 200, testPayloadString );
    assertTrue(response);
    clearDownData(1L);
  }

  private void clearDownData(Long expectedDeletions){
    Long deletions = caseSubmissionRepository.deleteByPayloadAndBatchIdAndTraceId(testPayloadString, testBatchId, testTraceId);
    assertEquals(expectedDeletions, deletions);
  }

  private CONTRIBUTIONS createTestContribution(){
    var contribution = new CONTRIBUTIONS();
    contribution.setId(BigInteger.valueOf(1000));
    contribution.setMaatId(BigInteger.valueOf(2000));
    return contribution;
  }

  private Fdc createTestFdc(){
    var fdc = new Fdc();
    fdc.setId(BigInteger.valueOf(1000));
    fdc.setMaatId(BigInteger.valueOf(2000));
    return fdc;
  }


}