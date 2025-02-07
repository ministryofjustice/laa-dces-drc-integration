package uk.gov.justice.laa.crime.dces.integration.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.Optional;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DrcApiWebClientConfiguration {
    private static final String SSL_BUNDLE_NAME = "drc-client";
    public static final String DRC_API_WEBCLIENT_REQUESTS = "webclient_requests";

    private final MeterRegistry meterRegistry;

    @Bean
    DrcClient drcClient(@Qualifier("drcApiWebClient") final WebClient drcApiWebClient) {
        final HttpServiceProxyFactory httpServiceProxyFactory =
                HttpServiceProxyFactory.builderFor(WebClientAdapter.create(drcApiWebClient)).build();
        return httpServiceProxyFactory.createClient(DrcClient.class);
    }

    /**
     * Constructs the managed Bean of class `WebClient` in the Spring context used to talk to the DRC API.
     *
     * @param webClientBuilder `WebClient.Builder` instance injected by Sprint Boot.
     * @param services Our services configuration properties (used to obtain the DRC API base URL).
     * @param sslBundles `SslBundles` instance injected by Spring Boot.
     * @param drcApiAuthorizedClientManager `OAuth2AuthorizedClientManager` instance configured in this class.
     * @return a configured `WebClient` instance.
     */
    @Bean("drcApiWebClient")
    public WebClient drcApiWebClient(final WebClient.Builder webClientBuilder,
                                     final ServicesProperties services,
                                     final SslBundles sslBundles,
                                     @Qualifier("drcApiAuthorizedClientManager")
                                     final OAuth2AuthorizedClientManager drcApiAuthorizedClientManager) {
        final ConnectionProvider provider = ConnectionProvider.builder("custom")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();
        // Clone Boot's auto-config WebClient.Builder, then add our customizations before build().
        WebClient.Builder builder = webClientBuilder.clone()
                .baseUrl(services.getDrcClientApi().getBaseUrl())
                .filter(new WebClientMetricsFilter(meterRegistry, DRC_API_WEBCLIENT_REQUESTS))
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(provider, sslBundles)));

        if (services.getDrcClientApi().isOAuthEnabled()) {
            if (drcApiAuthorizedClientManager != null) {
                final var oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(drcApiAuthorizedClientManager);
                oauth2.setDefaultClientRegistrationId("drc-client-api");
                builder.filter(oauth2);
            } else {
                log.warn("OAuth2 not enabled because no authorized client manager provided.");
            }
        }
        return builder.build();
    }

    @Bean("drcApiAuthorizedClientManager")
    public OAuth2AuthorizedClientManager drcApiAuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService clientService) {
        final var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                        .refreshToken() // is refresh_token needed for client_credentials?
                        .clientCredentials()
                        .build();
        final var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, clientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    /**
     * This method override doesn't create a managed Bean in the Spring context.
     * It exists to keep the unit tests compiling without adding another required parameter to them all.
     */
    public WebClient drcApiWebClient(final WebClient.Builder webClientBuilder,
                                     final ServicesProperties services) {
        return drcApiWebClient(webClientBuilder, services, null, null);
    }

    /**
     * Helper method to generate the netty `HttpClient` for the `WebClient` instance used to talk to the DRC API.
     *
     * @param provider Connection provider specified the connection pooling for clients - if not for this we could have
     *                 used `webClientBuilder.apply(webClientSsl.fromBundle(drcClientBundle))` instead of all this.
     * @param sslBundles Spring Boot's injected set of SSL bundles.
     * @return a configured `HttpClient` instance ready to use with `WebClient.Builder`.
     */
    private static HttpClient createHttpClient(final ConnectionProvider provider, final SslBundles sslBundles) {
        HttpClient httpClient = HttpClient.create(provider);
        final Optional<SslBundle> optBundle = getDrcClientBundle(sslBundles);
        if (optBundle.isPresent()) {
            httpClient = httpClient.secure(spec -> {
                SslOptions options = optBundle.get().getOptions();
                SslManagerBundle managers = optBundle.get().getManagers();
                SslContextBuilder builder = SslContextBuilder.forClient()
                        .keyManager(managers.getKeyManagerFactory())
                        .trustManager(managers.getTrustManagerFactory())
                        .ciphers(SslOptions.asSet(options.getCiphers()))
                        .protocols(options.getEnabledProtocols());
                try {
                    spec.sslContext(builder.build());
                } catch (SSLException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        return httpClient
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .compress(true)
                .responseTimeout(Duration.ofSeconds(30));
    }

    /**
     * Obtain the SslBundle used for drc-client TLS client authentication if it exists.
     *
     * @param sslBundles Spring Boot's injected `SslBundles` instance.
     * @return Optional containing the drc-client TLS client authentication SslBundle if it exists.
     */
    private static Optional<SslBundle> getDrcClientBundle(final SslBundles sslBundles) {
        if (sslBundles != null) {
            try {
                final SslBundle sslBundle = sslBundles.getBundle(SSL_BUNDLE_NAME);
                if (sslBundle == null) { // This is currently unneeded, but in case the API changes in the future.
                    throw new NoSuchSslBundleException(SSL_BUNDLE_NAME, "getBundle returned null");
                }
                log.info("SSL Bundle '{}' retrieved: {}", SSL_BUNDLE_NAME, sslBundle);
                return Optional.of(sslBundle);
            } catch (NoSuchSslBundleException e) {
                log.info("SSL Bundle '{}' not available: {}", e.getBundleName(), e.getMessage());
            }
        } else {
            log.info("SSL Bundles not available: sslBundles == null");
        }
        return Optional.empty();
    }
}