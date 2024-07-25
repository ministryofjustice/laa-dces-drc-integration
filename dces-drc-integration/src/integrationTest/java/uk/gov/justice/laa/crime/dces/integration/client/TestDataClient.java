package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PutExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionResponseDTO;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionFileErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionFileResponse;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;

import java.util.List;

public interface TestDataClient extends MaatApiClient {
    @PutExchange("/debt-collection-enforcement/concor-contribution-status")
    @Valid
    List<Integer> updateConcorContributionStatus(@RequestBody UpdateConcorContributionStatusRequest updateConcorContributionStatusRequest);

    @GetExchange("/debt-collection-enforcement/concor-contribution/{id}")
    @Valid
    ConcorContributionResponseDTO getConcorContribution(@PathVariable Integer id);

    @GetExchange("/debt-collection-enforcement/fdc-contribution/{fdcContributionId}")
    @Valid
    FdcContribution getFdcContribution(@PathVariable Integer fdcContributionId);

    @GetExchange("/debt-collection-enforcement/contribution-file/{contributionFileId}")
    @Valid
    ContributionFileResponse getContributionFile(@PathVariable int contributionFileId);

    @GetExchange("/debt-collection-enforcement/contribution-file/{contributionFileId}/error/{contributionId}")
    @Valid
    ContributionFileErrorResponse getContributionFileError(@PathVariable int contributionFileId, @PathVariable int contributionId);

    @Configuration
    class TestDataClientFactory {

        @Bean
        public TestDataClient getTestDataClient(WebClient maatApiWebClient) {
            return MaatApiClientFactory.maatApiClient(maatApiWebClient, TestDataClient.class);
        }
    }

}
