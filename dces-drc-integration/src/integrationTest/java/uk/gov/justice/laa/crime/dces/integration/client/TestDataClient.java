package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;

import java.util.List;

@HttpExchange("/debt-collection-enforcement/test-data")
public interface TestDataClient extends MaatApiClient {
    @PutExchange("/concor-contribution-status")
    @Valid
    List<Long> updateConcurContributionStatus(@RequestBody UpdateConcorContributionStatusRequest updateConcorContributionStatusRequest);

    @Configuration
    class TestDataClientFactory {

        @Bean
        public TestDataClient getTestDataClient(WebClient maatApiWebClient) {
            return MaatApiClientFactory.maatApiClient(maatApiWebClient, TestDataClient.class);
        }
    }

}
