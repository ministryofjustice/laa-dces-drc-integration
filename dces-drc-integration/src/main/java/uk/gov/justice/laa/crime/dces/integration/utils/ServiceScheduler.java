package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;

import java.time.LocalDateTime;

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
    @Value("${scheduling.fdcDailyFiles.cron}")
    private  String cronValue;
    @Scheduled(cron =  "${scheduling.fdcDailyFiles.cron}")
    public void processFdcDailyFiles()
    {
        log.info("Processing fdc daily files at {} with cron {}", LocalDateTime.now(), cronValue);
        fdcService.processDailyFiles();
    }

    @Scheduled(cron = "${scheduling.contributionsDailyFiles.cron}")
    public void processContributionsDailyFiles()
    {
        log.info("Processing contributions daily files at {} with cron {}", LocalDateTime.now(), cronValue);
        contributionService.processDailyFiles();
    }
}
