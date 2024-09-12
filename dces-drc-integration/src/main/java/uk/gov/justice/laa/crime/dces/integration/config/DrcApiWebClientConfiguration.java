package uk.gov.justice.laa.crime.dces.integration.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;

@Slf4j
@Configuration
@AllArgsConstructor
public class DrcApiWebClientConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        // Default serialization of XMLGregorianCalendar calls #toCalendar() to convert it to a Calendar instance, then
        // serializes that as a number or a full ISO8601 timestamp string (depending on  WRITE_DATES_AS_TIMESTAMPS).
        // By overriding to use ToStringSerializer, #toString() calls #toXMLFormat(), which takes into account undefined
        // fields, so does not include the 'T00:00:00Z' time part for dates, for example.
        return builder -> builder.serializerByType(XMLGregorianCalendar.class, ToStringSerializer.instance);
    }

    @Bean
    public WebClient drcApiWebClient(
            WebClient.Builder webClientBuilder,
            ServicesConfiguration servicesConfiguration
    ) {
        ConnectionProvider provider = ConnectionProvider.builder("custom")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        return webClientBuilder.clone() // clone Boot's auto-config WebClient.Builder, then add our customizations before build().
                .baseUrl(servicesConfiguration.getDrcClientApi().getBaseUrl())
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create(provider)
                                        .resolver(DefaultAddressResolverGroup.INSTANCE)
                                        .compress(true)
                                        .responseTimeout(Duration.ofSeconds(30))))
                .build();
    }

    @Bean
    DrcClient crimeApplyDatastoreClient(WebClient drcApiWebClient) {
        HttpServiceProxyFactory httpServiceProxyFactory =
                HttpServiceProxyFactory.builderFor(WebClientAdapter.create(drcApiWebClient)).build();
        return httpServiceProxyFactory.createClient(DrcClient.class);
    }
}