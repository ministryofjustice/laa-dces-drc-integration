package uk.gov.justice.laa.crime.dces.integration.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestConfig;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class MaatApiWebClientFactoryTest extends ApplicationTestConfig {

    MaatApiWebClientFactory maatApiWebClientFactory;
    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private WebClient.Builder webClientBuilder;
    private MockWebServer mockWebServer;

    @Qualifier("servicesConfiguration")
    @Autowired
    private ServicesConfiguration configuration;
    @MockBean
    OAuth2AuthorizedClientManager authorizedClientManager;

    @BeforeEach
    public void setup() throws IOException {

        mockWebServer = new MockWebServer();
        mockWebServer.start();
        configuration.getMaatApi().setBaseUrl(String.format("http://localhost:%s", mockWebServer.getPort()));

        maatApiWebClientFactory = new MaatApiWebClientConfiguration(meterRegistry);
    }

    @AfterEach
    void shutDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void givenAnyParameters_whenMaatApiWebClientIsInvoked_thenTheCorrectWebClientShouldBeReturned() throws JsonProcessingException {
        ConcorContribEntry expectedResponse = new ConcorContribEntry(1, "xmlContent");
        expectedResponse.setConcorContributionId(1);
        expectedResponse.setXmlContent("xmlContent");
        setupValidResponse(expectedResponse);

        WebClient actualWebClient = maatApiWebClientFactory.maatApiWebClient(webClientBuilder, configuration, authorizedClientManager);

        assertThat(actualWebClient).isNotNull();
        assertThat(actualWebClient).isInstanceOf(WebClient.class);

        ConcorContribEntry response = mockWebClientRequest(actualWebClient);
        assert response != null;
        assertThat(response).isInstanceOf(ConcorContribEntry.class);
        assertThat(response.getConcorContributionId()).isEqualTo(expectedResponse.getConcorContributionId());
    }

    private <T> void setupValidResponse(T returnBody) throws JsonProcessingException {
        String responseBody = mapper.writeValueAsString(returnBody);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(responseBody)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    private ConcorContribEntry mockWebClientRequest(WebClient webClient) {
        return webClient
                .get()
                .uri(configuration.getMaatApi().getBaseUrl())
                .retrieve()
                .bodyToMono(ConcorContribEntry.class)
                .block();
    }
}
