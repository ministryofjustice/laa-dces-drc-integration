package uk.gov.justice.laa.crime.dces.integration.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@EnableConfigurationProperties(value = ServicesProperties.class)
class ServicesPropertiesTest extends ApplicationTestBase {
    @Autowired
    private ServicesProperties services;

    @Autowired
    Environment env;

    @Test
    void givenDefinedBasedURL_whenGetBaseUrlIsInvoked_thenCorrectBaseURLIsReturned() {
        assertThat(services.getMaatApi().getBaseUrl()).isEqualTo("http://localhost:1111");
    }

    @Test
    void givenDefinedBasedURL_whenGetBaseUrlIsInvoked_thenDrcApiBaseURLIsReturned() {
        assertThat(services.getDrcClientApi().getBaseUrl()).isEqualTo("http://localhost:2222");
    }
}