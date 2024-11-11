package uk.gov.justice.laa.crime.dces.integration.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MaatApiWebClientConfiguration {
    private static final String LAA_TRANSACTION_ID = "LAA-TRANSACTION-ID";
    public static final String MAAT_API_WEBCLIENT_REQUESTS = "webclient_requests";

    private final MeterRegistry meterRegistry;

    @Bean
    public ContributionClient contributionClient(@Qualifier("maatApiWebClient") WebClient maatApiWebClient) {
        return MaatApiClientFactory.maatApiClient(maatApiWebClient, ContributionClient.class);
    }

    @Bean
    public FdcClient fdcClient(@Qualifier("maatApiWebClient") WebClient maatApiWebClient) {
        return MaatApiClientFactory.maatApiClient(maatApiWebClient, FdcClient.class);
    }

    /**
     * Constructs the managed Bean of class `WebClient` in the Spring context used to talk to the MAAT API.
     *
     * @param webClientBuilder `WebClient.Builder` instance injected by Sprint Boot.
     * @param services Our services configuration properties (used to obtain the MAAT API base URL).
     * @param authorizedClientManager OAuth configuration injected by Spring Boot.
     * @return a configured `WebClient` instance.
     */
    @Bean("maatApiWebClient")
    public WebClient maatApiWebClient(
            WebClient.Builder webClientBuilder,
            ServicesProperties services,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        ConnectionProvider provider = ConnectionProvider.builder("custom")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // Clone Boot's auto-config WebClient.Builder, then add our customizations before build().
        WebClient.Builder builder = webClientBuilder.clone()
            .baseUrl(services.getMaatApi().getBaseUrl())
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

        if (services.getMaatApi().isOAuthEnabled()) {
            ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                    new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

            oauth2Client.setDefaultClientRegistrationId(
                    services.getMaatApi().getRegistrationId()
            );

            builder.filter(oauth2Client);
        }

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(
                convertMaxBufferSize(services.getMaatApi().getMaxBufferSize())
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

    // TODO: Eventually remove this filter function and rely on default WebClientResponseException subclasses.
    private ExchangeFilterFunction handleErrorResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(
                clientResponse -> {
                    if (!clientResponse.statusCode().isError()) {
                        return Mono.just(clientResponse);
                    }
                    HttpStatus httpStatus = HttpStatus.resolve(clientResponse.statusCode().value());
                    if (httpStatus == null) { // can be null if remote end returns an invalid statusCode
                        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                    }
                    String errorMessage = String.format("Received error %s due to %s",
                            clientResponse.statusCode(), httpStatus.getReasonPhrase()
                    );

                    if (httpStatus.is5xxServerError()) {
                        return Mono.error(new HttpServerErrorException(httpStatus, errorMessage));
                    }

                    // TODO: Gradually move all statuses to the subclasses of WebClientResponseException that this creates:
                    if (httpStatus.equals(HttpStatus.NOT_FOUND) || httpStatus.equals(HttpStatus.CONFLICT)) {
                        return clientResponse.createError();
                    }

                    return Mono.error(new MaatApiClientException(httpStatus, errorMessage));
                }
        );
    }

    ExchangeFilterFunction addLaaTransactionIdToRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            //TODO: getting the value from the Trace Service and pass this to the MAAT API
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
