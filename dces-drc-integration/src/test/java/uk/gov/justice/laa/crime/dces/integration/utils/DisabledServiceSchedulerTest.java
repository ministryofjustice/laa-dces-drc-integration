package uk.gov.justice.laa.crime.dces.integration.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.service.MigrationService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(ServiceScheduler.class)
@TestPropertySource(properties = {
        "scheduling.fdcDailyFiles.cron=-",
        "scheduling.contributionsDailyFiles.cron=-"
})
@ActiveProfiles(profiles = "default")
class DisabledServiceSchedulerTest {

    @MockBean
    private FdcService fdcService;
    @MockBean
    private MigrationService migrationService;
    @MockBean
    private ContributionService contributionService;
    @InjectMocks
    private ServiceScheduler serviceScheduler;

    @Test
    void testProcessFdcDailyFilesIsNotCalled() throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(1000);
        verify(fdcService, never()).processDailyFiles();
    }

    @Test
    void testProcessContributionsDailyFilesIsNotCalled() throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(1000);
        verify(contributionService, never()).processDailyFiles();
    }
}