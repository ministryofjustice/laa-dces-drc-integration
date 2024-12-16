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
import uk.gov.justice.laa.crime.dces.integration.service.MigrationService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

  @Autowired
  private MigrationService migrationService;

  private static final Long TEST_BATCH_ID = -999L;

  // Method to clear out any lingering test data that might exist.
  @AfterAll
  @BeforeAll
  public void deleteTestData(){
    caseMigrationRepository.deleteByBatchId(TEST_BATCH_ID);
  }

  private void clearDownData(Long expectedDeletions){
    clearDownData(expectedDeletions, TEST_BATCH_ID);
  }
  private void clearDownData(Long expectedDeletions, Long batchId){
    Long deletions = caseMigrationRepository.deleteByBatchId(batchId);
    assertEquals(expectedDeletions, deletions, String.format("Unexpected Data Deletions: Wanted:%s, got %s", expectedDeletions, deletions));
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

    var byBatchAndTypeResponse = caseMigrationRepository.getCaseMigrationEntitiesByBatchIdAndRecordTypeAndIsProcessed(TEST_BATCH_ID, CONTRIBUTION.getName(), true);

    assertNotNull(byBatchAndTypeResponse);
    assertEquals(3, byBatchAndTypeResponse.size());

    clearDownData(4L);
  }

  @Test
  void given_maxBatch_willReturn(){
    Long initialMaxBatch = migrationService.getMaxBatch();
    Long expectedMaxBatch = 1000000L;
    clearDownData(0L, expectedMaxBatch);
    CaseMigrationEntity migrationEntity = createTestMigrationEntry(-1000, CONTRIBUTION);
    migrationEntity.setBatchId(expectedMaxBatch);
    caseMigrationRepository.save(migrationEntity);
    Long newMaxBatch = migrationService.getMaxBatch();
    assertNotEquals(initialMaxBatch, newMaxBatch);
    assertEquals(newMaxBatch, expectedMaxBatch);
    clearDownData(1L, expectedMaxBatch);
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