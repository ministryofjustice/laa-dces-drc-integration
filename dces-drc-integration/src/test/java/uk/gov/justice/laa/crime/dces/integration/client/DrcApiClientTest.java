package uk.gov.justice.laa.crime.dces.integration.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestConfig;
import uk.gov.justice.laa.crime.dces.integration.config.DrcApiWebClientConfiguration;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.failBecauseExceptionWasNotThrown;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class DrcApiClientTest extends ApplicationTestConfig {
    public DrcApiWebClientConfiguration drcApiWebClientConfiguration;
    private MockWebServer mockWebServer;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ObjectMapper mapper;

    @Qualifier("servicesConfiguration")
    @Autowired
    private ServicesConfiguration configuration;

    @BeforeEach
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(0);
        configuration.getDrcClientApi().setBaseUrl(String.format("http://localhost:%s", mockWebServer.getPort()));
        drcApiWebClientConfiguration = new DrcApiWebClientConfiguration();
    }

    @AfterEach
    void shutDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void test_whenWebClientIsRequested_thenDrcWebClientIsReturned() {

        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(webClientBuilder, configuration);

        assertThat(actualWebClient).isNotNull();
        assertThat(actualWebClient).isInstanceOf(WebClient.class);
    }

    @Test
    void test_whenWebClientIsInvoked_thenSuccessfulResponse() throws InterruptedException, DatatypeConfigurationException {

        ConcorContributionReqForDrc concorContributionReqForDrc = ConcorContributionReqForDrc.of(99, fakeCONTRIBUTIONS());
        setupSuccessfulResponse();
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(webClientBuilder, configuration);

        ResponseEntity<Void> response = callDrcClient(actualWebClient, concorContributionReqForDrc);
        String body = mockWebServer.takeRequest().getBody().readUtf8();
        assertThat(body).matches("\\{\"data\":\\{\"concorContributionId\":99,\"concorContributionObj\":\\{.+}},\"meta\":\\{}}");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void test_whenWebClientIsInvokedWithMissingMaatId_thenErrorResponse() throws JsonProcessingException, InterruptedException {

        ConcorContributionReqForDrc concorContributionReqForDrc = ConcorContributionReqForDrc.of(0, new CONTRIBUTIONS());
        setupProblemDetailResponse(fakeProblemDetail());
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(webClientBuilder, configuration);
        try {
            callDrcClient(actualWebClient, concorContributionReqForDrc);
            failBecauseExceptionWasNotThrown(WebClientResponseException.class);
        } catch (WebClientResponseException e) {
            String body = mockWebServer.takeRequest().getBody().readUtf8();
            assertThat(body).matches("\\{\"data\":\\{\"concorContributionId\":0,\"concorContributionObj\":\\{.*}},\"meta\":\\{}}");
            assertThat(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError()).isTrue();
        }
    }

    @Test
    void test_whenFdcWebClientIsInvoked_thenSuccessfulResponse() throws InterruptedException, DatatypeConfigurationException {

        FdcReqForDrc request = FdcReqForDrc.of(99, fakeFdc());
        setupSuccessfulResponse();
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(webClientBuilder, configuration);

        ResponseEntity<Void> response = callDrcClient(actualWebClient, request);
        String body = mockWebServer.takeRequest().getBody().readUtf8();
        assertThat(body).matches("\\{\"data\":\\{\"fdcId\":99,\"fdcObj\":\\{.+}},\"meta\":\\{}}");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void test_whenFdcWebClientIsInvokedWithMissingMaatId_thenErrorResponse() throws JsonProcessingException, InterruptedException {

        FdcReqForDrc request = FdcReqForDrc.of(0, new FdcFile.FdcList.Fdc());
        setupProblemDetailResponse(fakeProblemDetail());
        WebClient actualWebClient = drcApiWebClientConfiguration.drcApiWebClient(webClientBuilder, configuration);

        try {
            callDrcClient(actualWebClient, request);
            failBecauseExceptionWasNotThrown(WebClientResponseException.class);
        } catch (WebClientResponseException e) {
            String body = mockWebServer.takeRequest().getBody().readUtf8();
            assertThat(body).matches("\\{\"data\":\\{\"fdcId\":0,\"fdcObj\":\\{.*}},\"meta\":\\{}}");
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

    private CONTRIBUTIONS fakeCONTRIBUTIONS() throws DatatypeConfigurationException {
        var factory = new uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ObjectFactory();
        var contribution = factory.createCONTRIBUTIONS();
        contribution.setId(BigInteger.valueOf(3333));
        contribution.setMaatId(BigInteger.valueOf(3338));
        contribution.setFlag("NEW");
        var applicant = factory.createCONTRIBUTIONSApplicant();
        applicant.setFirstName("John");
        applicant.setLastName("Smith");
        var cal = DatatypeFactory.newInstance().newXMLGregorianCalendar("1970-12-31");
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

    private FdcFile.FdcList.Fdc fakeFdc() throws DatatypeConfigurationException {
        var factory = new uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.ObjectFactory();
        var fdc = factory.createFdcFileFdcListFdc();
        fdc.setId(BigInteger.valueOf(12));
        fdc.setMaatId(BigInteger.valueOf(16));
        fdc.setAgfsTotal(BigDecimal.valueOf(1000.00));
        fdc.setLgfsTotal(BigDecimal.valueOf(2000.00));
        fdc.setFinalCost(BigDecimal.valueOf(3000.00));
        var cal = DatatypeFactory.newInstance().newXMLGregorianCalendar("2024-09-25");
        fdc.setSentenceDate(cal);
        fdc.setCalculationDate(cal);
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
        final String responseBody = mapper.writeValueAsString(problemDetail);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(problemDetail.getStatus())
                .setBody(responseBody)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }
}
