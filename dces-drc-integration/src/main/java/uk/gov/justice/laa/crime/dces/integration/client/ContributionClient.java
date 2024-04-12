package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogFdcRequest;

import java.util.List;


@HttpExchange("/debt-collection-enforcement")
public interface ContributionClient extends MaatApiClient {
    @GetExchange("/concor-contribution-files")
    List<ConcurContribEntry> getContributions(@RequestParam String status);

    @PostExchange("/create-contribution-file")
    @Valid
    Boolean updateContributions(@RequestBody ContributionUpdateRequest contributionUpdateRequest);

    @PostExchange("/prepare-fdc-contributions-files")
    FdcGlobalUpdateResponse executeFdcGlobalUpdate();

    @GetExchange("/fdc-contribution-files")
    FdcContributionsResponse getFdcContributions(@RequestParam String status);

    @PostExchange("/create-fdc-file")
    @Valid
    Boolean updateFdcs(@RequestBody FdcUpdateRequest contributionPutRequest);


    @PostExchange("/log-contribution-response")
    @Valid
    Boolean sendLogContributionProcessed(@RequestBody UpdateLogContributionRequest updateLogContributionRequest);

    @PostExchange("/log-fdc-response")
    @Valid
    Boolean sendLogFdcProcessed(@RequestBody UpdateLogFdcRequest updateLogFdcRequest);


    @Configuration
    class ContributionFilesClientFactory {

        @Bean
        public ContributionClient getContributionFilesClient(WebClient maatApiWebClient) {
            return MaatApiClientFactory.maatApiClient(maatApiWebClient, ContributionClient.class);
        }
    }
}
