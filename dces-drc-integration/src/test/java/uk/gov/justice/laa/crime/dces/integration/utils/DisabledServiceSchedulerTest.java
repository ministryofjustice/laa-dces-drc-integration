package uk.gov.justice.laa.crime.dces.integration.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionFileService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcFileService;
import uk.gov.justice.laa.crime.dces.integration.service.MigrationService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(ServiceScheduler.class)
@TestPropertySource(properties = {
        "scheduling.fdcDailyFiles.cron=-",
        "scheduling.contributionsDailyFiles.cron=-",
        "scheduling.cron.data-migration=-",
        "scheduling.cron.purge.data-cleardown=-",
        "scheduling.cron.purge.drc-processing-status=-"
})
@ActiveProfiles(profiles = "default")
class DisabledServiceSchedulerTest {

    @MockitoBean
    private FdcFileService fdcFileService;
    @MockitoBean
    private MigrationService migrationService;
    @MockitoBean
    private ContributionFileService contributionFileService;
    @MockitoBean
    private EventService eventService;
    @InjectMocks
    private ServiceScheduler serviceScheduler;

    @Test
    void testProcessFdcDailyFilesIsNotCalled() throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(1000);
        verify(fdcFileService, never()).processDailyFiles();
    }

    @Test
    void testProcessContributionsDailyFilesIsNotCalled() throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(1000);
        verify(contributionFileService, never()).processDailyFiles();
    }

    @Test
    void testDataMigrationIsNotCalled() throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(1000);
        verify(migrationService, never()).migration();
    }

    @Test
    void testDatasourceCleardownIsNotCalled() throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(1000);
        verify(eventService, never()).purgePeriodicCaseSubmissionEntries();
    }

    @Test
    void givenAInvalidCronJob_shouldNotInvoked_PurgePeriodicDrcProcessingStatusEntries() throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(1000);
        verify(eventService, never()).purgePeriodicDrcProcessingStatusEntries();
    }
}