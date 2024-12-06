package uk.gov.justice.laa.crime.dces.integration.testing;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseMigrationEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseMigrationRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType.CONTRIBUTION;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType.FDC;


@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationDbLoggingTest {

  @Autowired
  private CaseMigrationRepository caseMigrationRepository;

  private static final Long TEST_BATCH_ID = -999L;

  // Method to clear out any lingering test data that might exist.
  @AfterAll
  @BeforeAll
  public void deleteTestData(){
    caseMigrationRepository.deleteByBatchId(TEST_BATCH_ID);
  }

  @Test
  void given_AllRequiredValues_thenSaves(){
    var response = caseMigrationRepository.save(createTestMigrationEntry(-1000L, CONTRIBUTION));
    assertNotNull(response);
    clearDownData(1L);
  }

  @Test
  void given_MultipleEntries_thenAllBatchReturned(){
    caseMigrationRepository.save(createTestMigrationEntry(-1000L, CONTRIBUTION));
    caseMigrationRepository.save(createTestMigrationEntry(-2000L, CONTRIBUTION));
    caseMigrationRepository.save(createTestMigrationEntry(-3000L, CONTRIBUTION));
    caseMigrationRepository.save(createTestMigrationEntry(-4000L, FDC));

    var byBatchResponse = caseMigrationRepository.getCaseMigrationEntitiesByBatchId(TEST_BATCH_ID);

    assertNotNull(byBatchResponse);
    assertEquals(4,byBatchResponse.size());

    var byBatchAndTypeResponse = caseMigrationRepository.getCaseMigrationEntitiesByBatchIdAndRecordType(TEST_BATCH_ID, CONTRIBUTION.getName());

    assertNotNull(byBatchAndTypeResponse);
    assertEquals(3, byBatchAndTypeResponse.size());

    clearDownData(4L);
  }

  private void clearDownData(Long expectedDeletions){
    Long deletions = caseMigrationRepository.deleteByBatchId(TEST_BATCH_ID);
    assertEquals(expectedDeletions, deletions);
  }

  private CaseMigrationEntity createTestMigrationEntry(long maatId, RecordType recordType){
    return CaseMigrationEntity.builder()
            .batchId(TEST_BATCH_ID)
            .recordType(recordType.getName())
            .maatId(maatId)
            .concorContributionId(-2000L)
            .fdcId(-3000L)
            .payload("Test Data, Ignore")
            .httpStatus(200)
            .isProcessed(true)
            .processedDate(LocalDateTime.now())
            .build();
  }


}