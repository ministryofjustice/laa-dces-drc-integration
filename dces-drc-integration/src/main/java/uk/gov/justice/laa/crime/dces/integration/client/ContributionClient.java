package uk.gov.justice.laa.crime.dces.integration.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;

import java.util.List;


@HttpExchange("/debt-collection-enforcement")
public interface ContributionClient extends MaatApiClient {

    @GetExchange("/contributions")
    List<ConcurContribEntry> getContributions();

    @PostExchange("/contributions")
    Object updateContributions();

    @Configuration
    class ContributionFilesClientFactory {

        @Bean
        public ContributionClient getContributionFilesClient(WebClient maatApiWebClient) {
            return MaatApiClientFactory.maatApiClient(maatApiWebClient, ContributionClient.class);
        }
    }
}
