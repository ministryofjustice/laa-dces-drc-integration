package uk.gov.justice.laa.crime.dces.integration.config;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClientBase;

/**
 * This is not a Spring `@Configuration` class, but just a POJO Factory class.
 * So the methods should not be annotated `@Bean`. Instead, it is used directly by `MaatApiWebClientConfiguration`
 * (which is an `@Configuration` class) to generate the Spring context beans that subclass `MaatApiClientBase`.
 */
public class MaatApiClientFactory {
    private MaatApiClientFactory(){
        throw new UnsupportedOperationException("Utility Class");
    }

    public static <T extends MaatApiClientBase> T maatApiClient(WebClient maatApiWebClient, Class<T> returnClass) {
        HttpServiceProxyFactory httpServiceProxyFactory =
                HttpServiceProxyFactory.builderFor(WebClientAdapter.create(maatApiWebClient))
                        .build();
        return httpServiceProxyFactory.createClient(returnClass);
    }
}
