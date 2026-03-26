package uk.gov.justice.laa.crime.dces.integration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

@Slf4j
public class WebClientLoggingFilter {

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

/*
    public static ExchangeFilterFunction logRequestBody() {
      return (request, next) -> {

        // Request body is a Flux<DataBuffer>
        return DataBufferUtils.join(request.body())
            .flatMap(buffer -> {
              byte[] bytes = new byte[buffer.readableByteCount()];
              buffer.read(bytes);
              DataBufferUtils.release(buffer);

              String body = new String(bytes);
              log.info("Request Body: {}", body);

              // Wrap bytes into a new Flux<DataBuffer>
              Flux<DataBuffer> newBody = Flux.defer(() ->
                  Flux.just(request.exchangeStrategies().messageReaders().get(0)
                      .getDecoder()
                      .getDataBufferFactory()
                      .wrap(bytes))
              );

              // Build NEW request with the same headers, URL, method, etc.
              ClientRequest mutated = ClientRequest.create(request.method(), request.url())
                  .headers(h -> h.addAll(request.headers()))
                  .cookies(c -> c.addAll(request.cookies()))
                  .attributes(a -> a.putAll(request.attributes()))
                  .body(newBody)
                  .build();

              return next.exchange(mutated);
            });
      };
    }

  }
*/
  public static ExchangeFilterFunction logResponseBody() {
    return ExchangeFilterFunction.ofResponseProcessor(response -> {
      return response.bodyToMono(String.class)
          .flatMap(body -> {
            log.info("Response Body: {}", body);
            ClientResponse newResponse = ClientResponse
                .create(response.statusCode())
                .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                .body(body)
                .build();
            return Mono.just(newResponse);
          });
    });
  }

}