package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateRepOrder;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;

import java.util.List;


public interface TestDataClient extends MaatApiClient {
    @PutExchange("/debt-collection-enforcement/concor-contribution-status")
    @Valid
    List<Long> updateConcurContributionStatus(@RequestBody UpdateConcorContributionStatusRequest updateConcorContributionStatusRequest);

    @PutExchange("/debt-collection-enforcement/insert-fdc-items")
    @Valid
    void insertFdcItems(@RequestParam int fdcId, @RequestParam String fdcItemType, @RequestParam String adjustmentReason, @RequestParam String paidAsClaimed,
        @RequestParam String latestCostIndicator, @RequestParam String userCreated);

    @DeleteExchange("/debt-collection-enforcement/delete-crown-court-outcomes")
    @Valid
    void deleteCrownCourtOutcomes(@RequestParam Integer repOrderId);

    @PutExchange("/assessment/rep-orders")
    @Valid
    void updateRepOrderSentenceOrderDate(@RequestBody UpdateRepOrder updateRepOrder);

    @PostExchange("/debt-collection-enforcement/fdc-contribution")
    @Valid
    int createFdcContribution(@RequestBody FdcContribution fdcContribution);

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
