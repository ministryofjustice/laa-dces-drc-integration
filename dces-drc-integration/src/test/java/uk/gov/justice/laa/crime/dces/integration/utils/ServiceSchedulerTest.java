package uk.gov.justice.laa.crime.dces.integration.utils;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.justice.laa.crime.dces.integration.config.SchedulerConfiguration;
import uk.gov.justice.laa.crime.dces.integration.config.TestSchedulerConfiguration;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertAll;


@ExtendWith(OutputCaptureExtension.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(ServiceScheduler.class)
@SpringBootTest
@ContextConfiguration(classes = {TestSchedulerConfiguration.class})
@TestPropertySource(properties = {
        "scheduling.fdcDailyFiles.cron=* * * * * *",
        "scheduling.contributionsDailyFiles.cron=* * * * * *"
})
@ActiveProfiles(profiles = "default")
public class ServiceSchedulerTest {

    @MockBean
    private FdcService fdcService;

    @MockBean
    private ContributionService contributionService;
    @InjectMocks
    private ServiceScheduler serviceScheduler;

    @Test
    public void testProcessFdcDailyFilesIsCalled(CapturedOutput output) throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(2000);
        when(fdcService.processDailyFiles()).thenReturn(true);

        verify(fdcService, times(1)).processDailyFiles();
        assertThat(output.getOut()).contains("Processing fdc daily files");
    }

    @Test
    public void testProcessContributionsDailyFilesIsCalled(CapturedOutput output) throws InterruptedException {
        // Wait for the scheduled method to be called
        Thread.sleep(2000);
        when(contributionService.processDailyFiles()).thenReturn(true);

        verify(contributionService, times(1)).processDailyFiles();
        assertThat(output.getOut()).contains("Processing contributions daily files");
    }
}