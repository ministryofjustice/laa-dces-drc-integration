package uk.gov.justice.laa.crime.dces.integration.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

/**
 * Used by MaatApiWebClientConfiguration to add metrics to the MAAT API WebClient
 */
@RequiredArgsConstructor
public class WebClientMetricsFilter implements ExchangeFilterFunction {

    private final MeterRegistry meterRegistry;
    private final String metricsName;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        long start = System.nanoTime();
        return next.exchange(request)
                .doOnEach(signal -> {
                    if (signal.get() != null) {
                        Duration duration = Duration.ofNanos(System.nanoTime() - start);
                        HttpStatus status = HttpStatus.valueOf(Objects.requireNonNull(signal.get()).statusCode().value());
                        Timer.builder(metricsName)
                                .tag("method", request.method().toString())
                                .tag("uri", request.url().toString())
                                .tag("status", status.toString())
                                .tag("responseStatus", status.getReasonPhrase())
                                .publishPercentiles(0.5, 0.95, 0.99)
                                .register(meterRegistry)
                                .record(duration);
                    }
                });
    }
}