package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.service.MigrationService;

import java.time.LocalDateTime;

@Profile("!test")
@Component
@EnableScheduling
@EnableSchedulerLock(defaultLockAtLeastFor = "${scheduling.lock.at-least:PT0S}", defaultLockAtMostFor = "${scheduling.lock.at-most:PT15M}")
@RequiredArgsConstructor
@Slf4j
public class ServiceScheduler {
    private final FdcService fdcService;
    private final ContributionService contributionService;
    private final MigrationService migrationService;

    @Scheduled(cron =  "${scheduling.cron.process-fdc-files:-}")
    @SchedulerLock(name = "processFdcFiles")
    public void processFdcFiles() {
        LockAssert.assertLocked();
        log.info("Processing FDC files at {}", LocalDateTime.now());
        fdcService.processDailyFiles();
    }

    @Scheduled(cron = "${scheduling.cron.process-contributions-files:-}")
    @SchedulerLock(name = "processContributionsFiles")
    public void processContributionsFiles() {
        LockAssert.assertLocked();
        log.info("Processing contributions files at {}", LocalDateTime.now());
        contributionService.processDailyFiles();
    }

    @Scheduled(cron = "${scheduling.cron.data-migration:-}")
    @SchedulerLock(name = "dataMigration")
    public void dataMigration() throws InterruptedException {
        LockAssert.assertLocked();
        log.info("Starting Data Migration Process at {}", LocalDateTime.now());
        migrationService.migration();
    }
}
