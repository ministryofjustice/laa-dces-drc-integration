package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;

import java.time.LocalDateTime;

@Profile("!test")
@Component
@EnableScheduling
@Slf4j
public class ServiceScheduler {
    private final FdcService fdcService;
    private final ContributionService contributionService;

    public ServiceScheduler(FdcService fdcService, ContributionService contributionService) {
        this.fdcService = fdcService;
        this.contributionService = contributionService;
    }
    @Scheduled(cron =  "${scheduling.fdcDailyFiles.cron:-}")
    @SchedulerLock(name = "ProcessFdcDailyFiles_Scheduler", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void processFdcDailyFiles()
    {
        LockAssert.assertLocked();
        log.info("Processing fdc daily files at {}", LocalDateTime.now());
        fdcService.processDailyFiles();
    }

    @Scheduled(cron = "${scheduling.contributionsDailyFiles.cron:-}")
    @SchedulerLock(name = "ProcessContributionsDailyFiles_Scheduler", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void processContributionsDailyFiles()
    {
        LockAssert.assertLocked();
        log.info("Processing contributions daily files at {}", LocalDateTime.now());
        contributionService.processDailyFiles();
    }
}
