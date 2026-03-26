package uk.gov.justice.laa.crime.dces.integration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

public class WebClientLoggingFilter {

  private static final Logger log = LoggerFactory.getLogger(WebClientLoggingFilter.class);

  public static ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(request -> {
      log.info("Outgoing Request: {} {}", request.method(), request.url());
      request.headers().forEach((name, values) ->
          values.forEach(value -> log.info("{}={}", name, value))
      );
      return Mono.just(request);
    });
  }

  public static ExchangeFilterFunction logResponse() {
    return ExchangeFilterFunction.ofResponseProcessor(response -> {
      log.info("Response Status: {}", response.statusCode());
      return Mono.just(response);
    });
  }
}