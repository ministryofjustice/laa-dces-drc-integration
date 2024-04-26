package uk.gov.justice.laa.crime.dces.integration.client.external.drc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;
import uk.gov.justice.laa.crime.dces.integration.model.drc.DrcDataRequest;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DrcApiClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static MockWebServer mockWebServer;
    DrcApiWebClientConfiguration drcApiWebClientConfiguration;
    @Qualifier("servicesConfiguration")
    @Autowired
    private ServicesConfiguration configuration;

    @BeforeAll
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        configuration.getDrcClientApi().setBaseUrl(String.format("http://localhost:%s", mockWebServer.getPort()));
        drcApiWebClientConfiguration = new DrcApiWebClientConfiguration();
    }

    @AfterAll
    void shutDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void test_whenWebClientIsRequested_thenShouldDrcWebClientIsReturned() {

        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(configuration);

        assertThat(actualWebClient).isNotNull();
        assertThat(actualWebClient).isInstanceOf(WebClient.class);
    }

    @Test
    void test_whenWebClientIsInvoked_thenShouldReturnedValidResponse() throws JsonProcessingException {

        DrcDataRequest drcDataRequest = DrcDataRequest.builder()
                .contributionId(99)
                .build();
        Boolean expectedResponse = true;
        setupValidResponse(expectedResponse);
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(configuration);

        Boolean response = callDrcClient(actualWebClient, drcDataRequest);
        assertThat(response).isTrue();
    }

    @Test
    void test_whenWebClientIsInvokedWithNull_thenShouldReturnedFalseResponse() throws JsonProcessingException {

        DrcDataRequest drcDataRequest = DrcDataRequest.builder().build();
        Boolean expectedResponse = false;
        setupValidResponse(expectedResponse);
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(configuration);

        Boolean response = callDrcClient(actualWebClient, drcDataRequest);
        assertThat(response).isFalse();
    }

    private @Nullable Boolean callDrcClient(WebClient actualWebClient, DrcDataRequest drcDataRequest) {
        return actualWebClient
                .post()
                .uri(configuration.getDrcClientApi().getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(drcDataRequest)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
    }

    private <T> void setupValidResponse(T returnBody) throws JsonProcessingException {
        String responseBody = OBJECT_MAPPER.writeValueAsString(returnBody);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(responseBody)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }
}
