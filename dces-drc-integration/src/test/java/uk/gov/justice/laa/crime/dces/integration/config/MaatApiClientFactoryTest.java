package uk.gov.justice.laa.crime.dces.integration.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClientBase;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MaatApiClientFactoryTest extends ApplicationTestBase {

    @MockitoBean(name = "maatApiWebClient")
    WebClient maatApiWebClient;

    @Test
    void givenAnyParameters_whenMaatApiClientIsInvoked_thenTheCorrectClientShouldBeReturned() {
        MaatApiClientBase actualMaatApiClient = MaatApiClientFactory.maatApiClient(maatApiWebClient, MaatApiClientBase.class);
        assertThat(actualMaatApiClient).isNotNull();
        assertThat(actualMaatApiClient).isInstanceOf(MaatApiClientBase.class);
    }
}
