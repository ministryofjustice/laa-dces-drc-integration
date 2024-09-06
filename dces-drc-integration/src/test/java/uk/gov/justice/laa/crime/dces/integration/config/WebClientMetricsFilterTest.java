package uk.gov.justice.laa.crime.dces.integration.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebClientMetricsFilterTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    private MeterRegistry meterRegistry;

    @InjectMocks
    private WebClientMetricsFilter webClientMetricsFilter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        webClientMetricsFilter = new WebClientMetricsFilter(meterRegistry, "testMetrics");
    }

    @Test
    void testFilterFunction() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://dces.org")).build();
        ClientResponse response = ClientResponse.create(HttpStatus.OK).build();
        when(exchangeFunction.exchange(request)).thenReturn(Mono.just(response));

        webClientMetricsFilter.filter(request, exchangeFunction).block();

        verify(exchangeFunction).exchange(request);
        assertNotNull(meterRegistry.find("testMetrics").timer(), "Timer should be registered");
        assertEquals(1, meterRegistry.get("testMetrics").timer().count(), "Timer should record one event");
    }

    @Test
    void testErrorResponse() {
        WebClientMetricsFilter filter = new WebClientMetricsFilter(meterRegistry, "errorMetrics");

        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://example.com")).build();
        Mono<ClientResponse> errorResponse = Mono.error(new RuntimeException("Network error"));
        when(exchangeFunction.exchange(request)).thenReturn(errorResponse);

        assertThrows(RuntimeException.class, () -> {
            filter.filter(request, exchangeFunction).block();
        });
    }
}
