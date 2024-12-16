package uk.gov.justice.laa.crime.dces.integration.utils;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.inmemory.InMemoryLockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.service.MigrationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig({ServiceScheduler.class, ServiceSchedulerTest.TestSchedulerConfiguration.class})
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "scheduling.cron.process-fdc-files=* * * * * *",
        "scheduling.cron.process-contributions-files=* * * * * *",
        "scheduling.cron.data-migration=* * * * * *",
        "scheduling.lock.at-least=PT0S"
})
@ActiveProfiles(profiles = "default") // the ServiceScheduler is disabled during tests otherwise
public class ServiceSchedulerTest {

    @MockBean
    private FdcService fdcService;
    @MockBean
    private ContributionService contributionService;
    @MockBean
    private MigrationService migrationService;

    @InjectMocks
    private ServiceScheduler serviceScheduler;

    @Test
    public void testProcessFdcDailyFilesIsCalled(CapturedOutput output) throws InterruptedException {
        when(fdcService.processDailyFiles()).thenReturn(true); // Arrange

        Thread.sleep(1000); // Act - could be called once (or maybe twice) depending on timing

        verify(fdcService, atLeastOnce()).processDailyFiles(); // Assert
        verify(fdcService, atMost(2)).processDailyFiles();
        assertThat(output.getOut()).contains("Processing FDC files");
    }

    @Test
    public void testProcessContributionsDailyFilesIsCalled(CapturedOutput output) throws InterruptedException {
        when(contributionService.processDailyFiles()).thenReturn(true); // Arrange

        Thread.sleep(1000); // Act - could be called once (or maybe twice) depending on timing

        verify(contributionService, atLeastOnce()).processDailyFiles(); // Assert
        verify(contributionService, atMost(2)).processDailyFiles();
        assertThat(output.getOut()).contains("Processing contributions files");
    }

    static class DelayedTrue implements Answer<Boolean> {
        @Override
        public Boolean answer(InvocationOnMock invocation) throws Throwable {
            Thread.sleep(1010);
            return true;
        }
    }

    @Test
    void testProcessFdcDailyFilesIsLocked() throws InterruptedException {
        when(fdcService.processDailyFiles()).thenAnswer(new DelayedTrue()); // Arrange

        Thread.sleep(1990); // Act - should be called no more than once because of delay

        verify(fdcService, atMostOnce()).processDailyFiles(); // Assert
    }

    @Test
    void testProcessContributionsDailyFilesIsLocked() throws InterruptedException {
        when(contributionService.processDailyFiles()).thenAnswer(new DelayedTrue()); // Arrange

        Thread.sleep(1990); // Act - should be called no more than once because of delay

        verify(contributionService, atMostOnce()).processDailyFiles(); // Assert
    }

    @TestConfiguration
    static class TestSchedulerConfiguration {
        @Bean
        @Primary // This ensures this bean is used in tests instead of the main one
        public LockProvider lockProvider() {
            return new InMemoryLockProvider();
        }
    }
}
