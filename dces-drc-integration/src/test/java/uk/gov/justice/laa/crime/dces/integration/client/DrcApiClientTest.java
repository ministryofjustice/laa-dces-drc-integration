package uk.gov.justice.laa.crime.dces.integration.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.config.DrcApiWebClientConfiguration;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionDataForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcDataForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import java.io.IOException;
import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.failBecauseExceptionWasNotThrown;
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
    void test_whenWebClientIsRequested_thenDrcWebClientIsReturned() {

        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(configuration);

        assertThat(actualWebClient).isNotNull();
        assertThat(actualWebClient).isInstanceOf(WebClient.class);
    }

    @Test
    void test_whenWebClientIsInvoked_thenSuccessfulResponse() throws InterruptedException {

        ContributionDataForDrc contributionDataForDrc = ContributionDataForDrc.of("99", fakeCONTRIBUTIONS());
        setupSuccessfulResponse();
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(configuration);

        ResponseEntity<Void> response = callDrcClient(actualWebClient, contributionDataForDrc);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getBody().readUtf8()).matches("\\{\"data\":\\{.+},\"meta\":\\{\"contributionId\":\"99\"}}");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void test_whenWebClientIsInvokedWithNull_thenErrorResponse() throws JsonProcessingException, InterruptedException {

        ContributionDataForDrc contributionDataForDrc = ContributionDataForDrc.of(null, null);
        setupProblemDetailResponse(fakeProblemDetail());
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(configuration);
        try {
            callDrcClient(actualWebClient, contributionDataForDrc);
            failBecauseExceptionWasNotThrown(WebClientResponseException.class);
        } catch (WebClientResponseException e) {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getBody().readUtf8()).matches("\\{\"data\":null,\"meta\":\\{}}");
            assertThat(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError()).isTrue();
        }
    }

    @Test
    void test_whenFdcWebClientIsInvoked_thenSuccessfulResponse() throws InterruptedException {

        FdcDataForDrc request = FdcDataForDrc.of("99", fakeFdc());
        setupSuccessfulResponse();
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(configuration);

        ResponseEntity<Void> response = callDrcClient(actualWebClient, request);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getBody().readUtf8()).matches("\\{\"data\":\\{.+},\"meta\":\\{\"fdcId\":\"99\"}}");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void test_whenFdcWebClientIsInvokedWithNull_thenErrorResponse() throws JsonProcessingException, InterruptedException {

        FdcDataForDrc request = FdcDataForDrc.of(null, null);
        setupProblemDetailResponse(fakeProblemDetail());
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(configuration);

        try {
            callDrcClient(actualWebClient, request);
            failBecauseExceptionWasNotThrown(WebClientResponseException.class);
        } catch (WebClientResponseException e) {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getBody().readUtf8()).matches("\\{\"data\":null,\"meta\":\\{}}");
            assertThat(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError()).isTrue();
        }
    }

    private <T> ResponseEntity<Void> callDrcClient(WebClient actualWebClient, T request) {
        return actualWebClient
                .post()
                .uri(configuration.getDrcClientApi().getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private CONTRIBUTIONS fakeCONTRIBUTIONS() {
        CONTRIBUTIONS contributions = new CONTRIBUTIONS();
        contributions.setId(BigInteger.valueOf(99));
        return contributions;
    }

    private FdcFile.FdcList.Fdc fakeFdc() {
        FdcFile.FdcList.Fdc fdc = new FdcFile.FdcList.Fdc();
        fdc.setId(BigInteger.valueOf(12));
        return fdc;
    }

    private ProblemDetail fakeProblemDetail() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    private void setupSuccessfulResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody("")
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    private void setupProblemDetailResponse(final ProblemDetail problemDetail) throws JsonProcessingException {
        final String responseBody = OBJECT_MAPPER.writeValueAsString(problemDetail);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(problemDetail.getStatus())
                .setBody(responseBody)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }
}
