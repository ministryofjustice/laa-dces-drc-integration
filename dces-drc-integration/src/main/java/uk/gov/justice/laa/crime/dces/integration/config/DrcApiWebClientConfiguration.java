package uk.gov.justice.laa.crime.dces.integration.config;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.DefaultAddressResolverGroup;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
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
public class DrcApiWebClientConfiguration {
    private static final String SSL_BUNDLE_NAME = "client-auth";

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
     * @param clientRegistrationRepository `ClientRegistrationRepository` instance injected by Spring Boot.
     * @param authorizedClientRepository `OAuth2AuthorizedClientRepository` instance injected by Spring Boot.
     * @return a configured `WebClient` instance.
     */
    @Bean("drcApiWebClient")
    public WebClient drcApiWebClient(final WebClient.Builder webClientBuilder,
                                     final ServicesProperties services,
                                     final SslBundles sslBundles,
                                     final ClientRegistrationRepository clientRegistrationRepository,
                                     final OAuth2AuthorizedClientRepository authorizedClientRepository) {
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
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(provider, sslBundles)));

        if (services.getDrcClientApi().isOAuthEnabled()) {
            if (clientRegistrationRepository != null && authorizedClientRepository != null) {
                final var oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrationRepository,
                                                                                           authorizedClientRepository);
                oauth2.setDefaultClientRegistrationId("drc-client-api");
                builder.apply(oauth2.oauth2Configuration());
            } else {
                log.warn("OAuth2 not enabled because no client registration or authorized client repository provided.");
            }
        }

        return builder.build();
    }

    /**
     * This method override doesn't create a managed Bean in the Spring context.
     * It exists to keep the unit tests compiling without adding another required parameter to them all.
     */
    public WebClient drcApiWebClient(final WebClient.Builder webClientBuilder,
                                     final ServicesProperties services) {
        return drcApiWebClient(webClientBuilder, services, null, null, null);
    }

    /**
     * Helper method to generate the netty `HttpClient` for the `WebClient` instance used to talk to the DRC API.
     *
     * @param provider Connection provider specified the connection pooling for clients - if not for this we could have
     *                 used `webClientBuilder.apply(webClientSsl.fromBundle(clientAuthBundle))` instead of all this.
     * @param sslBundles Spring Boot's injected set of SSL bundles.
     * @return a configured `HttpClient` instance ready to use with `WebClient.Builder`.
     */
    private static HttpClient createHttpClient(final ConnectionProvider provider, final SslBundles sslBundles) {
        HttpClient httpClient = HttpClient.create(provider);
        final Optional<SslBundle> optBundle = getClientAuthBundle(sslBundles);
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
     * Obtain the SslBundle used for client authentication if it exists.
     *
     * @param sslBundles Spring Boot's injected `SslBundles` instance.
     * @return Optional containing the client authentication SslBundle if it exists.
     */
    private static Optional<SslBundle> getClientAuthBundle(final SslBundles sslBundles) {
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