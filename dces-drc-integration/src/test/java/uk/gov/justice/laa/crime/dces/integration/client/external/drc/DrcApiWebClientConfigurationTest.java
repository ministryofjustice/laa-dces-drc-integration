package uk.gov.justice.laa.crime.dces.integration.client.external.drc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;

@TestConfiguration
class DrcApiWebClientConfigurationTest {

    @Bean
    WebClient drcApiWebClient(ServicesConfiguration servicesConfiguration) {
        return WebClient.builder().baseUrl(servicesConfiguration.getDrcClientApi().getBaseUrl()).build();
    }

    @Bean
    DrcClient drcApiWebClient(WebClient drcApiWebClient) {
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(drcApiWebClient)).build();
        return httpServiceProxyFactory.createClient(DrcClient.class);
    }
}