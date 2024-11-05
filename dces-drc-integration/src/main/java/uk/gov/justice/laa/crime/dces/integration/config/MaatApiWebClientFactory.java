package uk.gov.justice.laa.crime.dces.integration.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MaatApiWebClientFactory {
    private static final String LAA_TRANSACTION_ID = "LAA-TRANSACTION-ID";
    public static final String MAAT_API_WEBCLIENT_REQUESTS = "webclient_requests";

    private final MeterRegistry meterRegistry;

    @Bean
    public WebClient maatApiWebClient(
            WebClient.Builder webClientBuilder,
            ServicesConfiguration servicesConfiguration,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {

        ConnectionProvider provider = ConnectionProvider.builder("custom")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        WebClient.Builder builder = webClientBuilder.clone() // clone Boot's auto-config WebClient.Builder, then add our customizations before build().
            .baseUrl(servicesConfiguration.getMaatApi().getBaseUrl())
            .filter(addLaaTransactionIdToRequest())
            .filter(logClientResponse())
            .filter(handleErrorResponse())
                .filter(new WebClientMetricsFilter(meterRegistry, MAAT_API_WEBCLIENT_REQUESTS))
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create(provider)
                    .resolver(DefaultAddressResolverGroup.INSTANCE)
                    .compress(true)
                    .responseTimeout(Duration.ofSeconds(90))
                )
            )
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (servicesConfiguration.getMaatApi().isOAuthEnabled()) {
            ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                    new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

            oauth2Client.setDefaultClientRegistrationId(
                    servicesConfiguration.getMaatApi().getRegistrationId()
            );

            builder.filter(oauth2Client);
        }

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(
                convertMaxBufferSize(servicesConfiguration.getMaatApi().getMaxBufferSize())
                ))
            .build();
        builder.exchangeStrategies(strategies);

        return builder.build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService clientService) {


        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .refreshToken()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, clientService
        );

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    private ExchangeFilterFunction handleErrorResponse() {
        return ExchangeFilterFunctions.statusError(
                HttpStatusCode::isError, clientResponse -> {
                    HttpStatus httpStatus =  HttpStatus.resolve(clientResponse.statusCode().value());
                    assert httpStatus != null;
                    String errorMessage = String.format("Received error %s due to %s",
                            clientResponse.statusCode(), httpStatus.getReasonPhrase()
                    );

                    if (httpStatus.is5xxServerError()) {
                        return new HttpServerErrorException(httpStatus, errorMessage);
                    }

                    if (httpStatus.equals(HttpStatus.NOT_FOUND)) {
                        return WebClientResponseException.create(
                                httpStatus.value(), httpStatus.getReasonPhrase(),
                                null, null, null);
                    }

                    return new MaatApiClientException(httpStatus, errorMessage);
                }
        );
    }

    ExchangeFilterFunction addLaaTransactionIdToRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            //TODO: getting the value from the Trace Serice and pass this to the Maat API
                String laaTransactionId = UUID.randomUUID().toString();
                log.info("LAA_TRANSACTION_ID=[{}] Calling API [{}]", laaTransactionId, clientRequest.url());
                return Mono.just(
                    ClientRequest
                        .from(clientRequest)
                        .header(LAA_TRANSACTION_ID, laaTransactionId)
                        .build()
                );
            }
        );
    }

    ExchangeFilterFunction logClientResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
                log.info("[{}] API response", clientResponse.statusCode());
                return Mono.just(clientResponse);
            }
        );
    }


    private static int convertMaxBufferSize(int megaBytes) {
        return megaBytes * 1024 * 1024;
    }

}