package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateRepOrder;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;

import java.util.List;


public interface TestDataClient extends MaatApiClient {
    @PutExchange("/debt-collection-enforcement/concor-contribution-status")
    @Valid
    List<Long> updateConcurContributionStatus(@RequestBody UpdateConcorContributionStatusRequest updateConcorContributionStatusRequest);

    @PostExchange("/debt-collection-enforcement/fdc-items")
    @Valid
    void createFdcItems(@Valid @RequestBody final FdcItem fdcItemDTO);

    @DeleteExchange("/assessment/rep-orders/cc-outcome/rep-order/{repId}")
    @Valid
    void deleteCrownCourtOutcomes(@PathVariable Integer repId);

    @PutExchange("/assessment/rep-orders")
    @Valid
    void updateRepOrderSentenceOrderDate(@RequestBody UpdateRepOrder updateRepOrder);

    @PostExchange("/debt-collection-enforcement/fdc-contribution")
    @Valid
    int createFdcContribution(@RequestBody CreateFdcContributionRequest fdcContribution);

    @GetExchange("/debt-collection-enforcement/rep-orders-eligible-for-min-delay-applies-fdc")
    @Valid
    List<Integer> getRepOrdersEligibleForMinDelayAppliesFDCs(@RequestParam int delayPeriod,@RequestParam String dateReceived, @RequestParam int numRecords);

    @GetExchange("/debt-collection-enforcement/rep-orders-eligible-for-min-delay-not-applies-fdc")
    @Valid
    List<Integer> getRepOrdersEligibleForMinDelayNotAppliesFDCs(@RequestParam int delayPeriod, @RequestParam String dateReceived, @RequestParam int numRecords);

    @Configuration
    class TestDataClientFactory {

        @Bean
        public TestDataClient getTestDataClient(WebClient maatApiWebClient) {
            return MaatApiClientFactory.maatApiClient(maatApiWebClient, TestDataClient.class);
        }
    }

}
