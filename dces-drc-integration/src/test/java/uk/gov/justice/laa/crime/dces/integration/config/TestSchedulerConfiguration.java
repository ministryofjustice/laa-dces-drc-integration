package uk.gov.justice.laa.crime.dces.integration.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.inmemory.InMemoryLockProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestSchedulerConfiguration {

    @Bean
    @Primary // This ensures this bean is used in tests instead of the main one
    public LockProvider lockProvider() {
        return new InMemoryLockProvider();
    }
}
