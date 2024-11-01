package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;

import java.time.LocalDate;
import java.util.List;

@HttpExchange("/debt-collection-enforcement")
public interface ContributionClient extends MaatApiClient {
    @GetExchange("/concor-contribution-files")
    List<ConcorContribEntry> getContributions(@RequestParam String status, @RequestParam Integer startingId, @RequestParam Integer numberOfRecords);

    @PostExchange("/create-contribution-file")
    @Valid
    Integer updateContributions(@RequestBody ContributionUpdateRequest contributionUpdateRequest);

    @PostExchange("/log-contribution-response")
    @Valid
    Integer sendLogContributionProcessed(@RequestBody UpdateLogContributionRequest updateLogContributionRequest);

    /** For testing only? */
    @GetExchange("/contributions")
    @Valid
    List<String> findContributionFiles(@RequestParam(name = "fromDate") @DateTimeFormat(pattern = "dd.MM.yyyy") final LocalDate fromDate,
                                       @RequestParam(name = "toDate") @DateTimeFormat(pattern = "dd.MM.yyyy") final LocalDate toDate);

    @Configuration
    class ContributionFilesClientFactory {
        @Bean
        public ContributionClient getContributionFilesClient(WebClient maatApiWebClient) {
            return MaatApiClientFactory.maatApiClient(maatApiWebClient, ContributionClient.class);
        }
    }
}
