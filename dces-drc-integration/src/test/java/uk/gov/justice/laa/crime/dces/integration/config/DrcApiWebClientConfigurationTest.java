package uk.gov.justice.laa.crime.dces.integration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest
class DrcApiWebClientConfigurationTest {

    @Mock
    private ServicesConfiguration servicesConfiguration;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private ServicesConfiguration.DrcClientApi drcClientApi;

    @InjectMocks
    private DrcApiWebClientConfiguration drcApiWebClientConfiguration;

    @BeforeEach
    void setUp() {
        when(servicesConfiguration.getDrcClientApi()).thenReturn(drcClientApi);
        when(servicesConfiguration.getDrcClientApi().getBaseUrl()).thenReturn("http://localhost:8080");
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void shouldReturnWebClientWhenDrcApiWebClientIsCalled() {
        WebClient result = drcApiWebClientConfiguration.drcApiWebClient(webClientBuilder, servicesConfiguration);
        assertNotNull(result);
    }

    @Test
    void shouldReturnDrcClientWhenCrimeApplyDatastoreClientIsCalled() {
        DrcClient result = drcApiWebClientConfiguration.crimeApplyDatastoreClient(webClient);
        assertNotNull(result);
    }
}