package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
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

    @PutExchange("/insert-fdc-items")
    @Valid
    void insertFdcItems(@RequestParam int fdcId);

    @PutExchange("/update-fdc-contribution-to-sent")
    @Valid
    void updateFdcContributions(@RequestParam Integer repOrderId, @RequestParam String previousStatus, @RequestParam String newStatus);

    @DeleteExchange("/delete-crown-court-outcomes")
    @Valid
    void deleteCrownCourtOutcomes(@RequestParam Integer repOrderId);

    @PutExchange("/update-rep-order-sentence-order-date")
    @Valid
    void updateRepOrderSentenceOrderDate(@RequestParam Integer repOrderId, @RequestParam int monthsAfterSysDate);

    @PutExchange("/create-fdc-contribution")
    @Valid
    int createFdcContribution(@RequestParam Integer repOrderId);

    @GetExchange("/rep-orders-eligible-for-min-delay-applies-fdc")
    @Valid
    List<Integer> getRepOrdersEligibleForMinDelayAppliesFDCs(@RequestParam int delayPeriod, @RequestParam String dateReceived, @RequestParam int numRecords);

    @Configuration
    class TestDataClientFactory {

        @Bean
        public TestDataClient getTestDataClient(WebClient maatApiWebClient) {
            return MaatApiClientFactory.maatApiClient(maatApiWebClient, TestDataClient.class);
        }
    }

}
