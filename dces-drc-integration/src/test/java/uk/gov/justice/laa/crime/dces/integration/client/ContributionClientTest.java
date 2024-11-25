package uk.gov.justice.laa.crime.dces.integration.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.config.MaatApiWebClientConfiguration;
import uk.gov.justice.laa.crime.dces.integration.config.ServicesProperties;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.failBecauseExceptionWasNotThrown;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class ContributionClientTest extends ApplicationTestBase {
    public MaatApiWebClientConfiguration maatApiWebClientConfiguration;
    private MockWebServer mockWebServer;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @Autowired
    private ServicesProperties services;

    @BeforeEach
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(0);
        services.getMaatApi().setBaseUrl(String.format("http://localhost:%s", mockWebServer.getPort()));
        maatApiWebClientConfiguration = new MaatApiWebClientConfiguration(meterRegistry);
    }

    @AfterEach
    void shutDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void test_whenWebClientIsRequested_thenDrcWebClientIsReturned() {

        WebClient actualWebClient = maatApiWebClientConfiguration.maatApiWebClient(webClientBuilder, services, oAuth2AuthorizedClientManager);

        assertThat(actualWebClient).isNotNull();
        assertThat(actualWebClient).isInstanceOf(WebClient.class);
    }

    @Test
    void test_whenWebClientIsInvoked_thenSuccessfulResponse() throws InterruptedException {

        ConcorContributionReqForDrc concorContributionReqForDrc = ConcorContributionReqForDrc.of(99L, fakeCONTRIBUTIONS());
        setupSuccessfulResponse();
        WebClient actualWebClient = maatApiWebClientConfiguration.maatApiWebClient(webClientBuilder, services, oAuth2AuthorizedClientManager);

        ResponseEntity<Void> response = callMaatClient(actualWebClient, concorContributionReqForDrc);
        String body = mockWebServer.takeRequest().getBody().readUtf8();
        assertThat(body).matches("\\{\"data\":\\{\"concorContributionId\":99,\"concorContributionObj\":\\{.+}},\"meta\":\\{}}");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void test_whenWebClientThrowsBadRequest_thenErrorResponse() throws JsonProcessingException, InterruptedException {
        setupErrorCodeTest(HttpStatus.BAD_REQUEST);
    }

    @Test
    void test_whenWebClientThrowsInternalError_thenErrorResponseIsCorrect() throws JsonProcessingException, InterruptedException {
        setupErrorCodeTest(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @Test
    void test_whenWebClientThrowsConflict_thenErrorResponseIsCorrect() throws JsonProcessingException, InterruptedException {
        setupErrorCodeTest(HttpStatus.CONFLICT);
    }
    @Test
    void test_whenWebClientThrowsNotFound_thenErrorResponseIsCorrect() throws JsonProcessingException, InterruptedException {
        setupErrorCodeTest(HttpStatus.NOT_FOUND);
    }

    private void setupErrorCodeTest(HttpStatus expectedStatus) throws JsonProcessingException, InterruptedException {
        ConcorContributionReqForDrc concorContributionReqForDrc = ConcorContributionReqForDrc.of(0L, new CONTRIBUTIONS());
        setupProblemDetailResponse(fakeProblemDetail(expectedStatus));
        WebClient actualWebClient = maatApiWebClientConfiguration.maatApiWebClient(webClientBuilder, services, oAuth2AuthorizedClientManager);
        try {
            callMaatClient(actualWebClient, concorContributionReqForDrc);
            failBecauseExceptionWasNotThrown(WebClientResponseException.class);
        } catch (WebClientResponseException e) {
            String body = mockWebServer.takeRequest().getBody().readUtf8();
            assertThat(body).matches("\\{\"data\":\\{\"concorContributionId\":0,\"concorContributionObj\":\\{.*}},\"meta\":\\{}}");
            assertThat(e.getStatusCode()).isEqualTo(expectedStatus);
        }
    }

    private <T> ResponseEntity<Void> callMaatClient(WebClient actualWebClient, T request) {
        return actualWebClient
                .post()
                .uri(services.getMaatApi().getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private CONTRIBUTIONS fakeCONTRIBUTIONS() {
        var factory = new uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ObjectFactory();
        var contribution = factory.createCONTRIBUTIONS();
        contribution.setId(3333L);
        contribution.setMaatId(3338L);
        contribution.setFlag("NEW");
        var applicant = factory.createCONTRIBUTIONSApplicant();
        applicant.setFirstName("John");
        applicant.setLastName("Smith");
        var cal = LocalDate.parse("1970-12-31");
        applicant.setDob(cal);
        applicant.setNiNumber("QQ999999Q");
        contribution.setApplicant(applicant);
        var assessment = factory.createCONTRIBUTIONSAssessment();
        assessment.setAssessmentDate(cal);
        assessment.setEffectiveDate(cal);
        assessment.setUpliftAppliedDate(cal);
        assessment.setUpliftRemovedDate(cal);
        contribution.setAssessment(assessment);
        return contribution;
    }

    private ProblemDetail fakeProblemDetail(HttpStatus httpStatusCode) {
        return ProblemDetail.forStatusAndDetail(httpStatusCode, httpStatusCode.getReasonPhrase());
    }

    private void setupSuccessfulResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody("")
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    private void setupProblemDetailResponse(final ProblemDetail problemDetail) throws JsonProcessingException {
        final String responseBody = mapper.writeValueAsString(problemDetail);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(problemDetail.getStatus())
                .setBody(responseBody)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }
}
