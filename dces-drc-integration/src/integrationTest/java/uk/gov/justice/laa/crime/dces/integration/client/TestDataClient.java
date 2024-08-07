package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionResponseDTO;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateRepOrder;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
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

    @PostExchange("/debt-collection-enforcement/fdc-items")
    @Valid
    void createFdcItems(@Valid @RequestBody final FdcItem fdcItemDTO);

    @DeleteExchange("/debt-collection-enforcement/fdc-items/fdc-id/{fdcId}")
    @Valid
    void deleteFdcItems(@NotNull @PathVariable final Integer fdcId);

    @DeleteExchange("/assessment/rep-orders/cc-outcome/rep-order/{repId}")
    @Valid
    void deleteCrownCourtOutcomes(@PathVariable Integer repId);

    @PutExchange("/assessment/rep-orders")
    @Valid
    void updateRepOrderSentenceOrderDate(@RequestBody UpdateRepOrder updateRepOrder);

    @PatchExchange("/assessment/rep-orders/{repId}")
    @Valid
    void updateRepOrderSentenceOrderDateToNull(@PathVariable Integer repId, @RequestBody Map<String, Object> repOrder);

    @PostExchange("/debt-collection-enforcement/fdc-contribution")
    @Valid
    FdcContribution createFdcContribution(@RequestBody CreateFdcContributionRequest fdcContribution);

    @PatchExchange("/debt-collection-enforcement/fdc-contribution")
    @Valid
    int updateFdcContribution(@RequestBody UpdateFdcContributionRequest fdcContribution);

    @GetExchange("/assessment/rep-orders")
    @Valid
    Set<Integer> getRepOrders(@RequestParam int delay, @RequestParam String dateReceived, @RequestParam int numRecords, @RequestParam boolean fdcDelayedPickup, @RequestParam boolean fdcFastTrack);

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
