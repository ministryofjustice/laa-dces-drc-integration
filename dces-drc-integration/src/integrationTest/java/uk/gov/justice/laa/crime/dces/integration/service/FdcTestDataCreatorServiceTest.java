package uk.gov.justice.laa.crime.dces.integration.service;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcTestType.NEGATIVE_CCO;
import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcTestType.NEGATIVE_FDC_ITEM;
import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcTestType.NEGATIVE_FDC_STATUS;
import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcTestType.NEGATIVE_SOD;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiWebClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FdcTestDataCreatorServiceTest {

  private static MockWebServer mockWebServer;
  @Qualifier("servicesConfiguration")
  @Autowired
  private ServicesConfiguration configuration;

  FdcTestDataCreatorService fdcTestDataCreatorService;

  @MockBean
  OAuth2AuthorizedClientManager authorizedClientManager;

  @BeforeAll
  public void setup() {
    mockWebServer = new MockWebServer();
    mockWebServer.setDispatcher(dispatcher);
    configuration.getMaatApi().setBaseUrl(String.format("http://localhost:%s", mockWebServer.getPort()));
    TestDataClient newTestDataClient = MaatApiClientFactory.maatApiClient(
        (new MaatApiWebClientFactory()).maatApiWebClient(configuration, authorizedClientManager),
        TestDataClient.class);
    fdcTestDataCreatorService = new FdcTestDataCreatorService(newTestDataClient);
  }

  @AfterAll
  void shutDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void test_whenCreateMinimumDelayAppliesTestDataNegativeSod_thenShouldUpdateSod()
      throws InterruptedException {
    fdcTestDataCreatorService.createMinimumDelayAppliesTestData(NEGATIVE_SOD, 3);
    checkRequest("GET", "/debt-collection-enforcement/test-data/rep-orders-eligible-for-min-delay-applies-fdc?delayPeriod=5&dateReceived=01-JAN-2015&numRecords=3");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/create-fdc-contribution?repOrderId=1");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/insert-fdc-items?fdcId=1001");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/update-rep-order-sentence-order-date?repOrderId=1&monthsAfterSysDate=3");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/create-fdc-contribution?repOrderId=2");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/insert-fdc-items?fdcId=2001");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/update-rep-order-sentence-order-date?repOrderId=2&monthsAfterSysDate=3");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/create-fdc-contribution?repOrderId=3");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/insert-fdc-items?fdcId=3001");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/update-rep-order-sentence-order-date?repOrderId=3&monthsAfterSysDate=3");
  }

  @Test
  void test_whenCreateMinimumDelayAppliesTestDataNegativeCco_thenShouldDeleteCco()
      throws InterruptedException {
    fdcTestDataCreatorService.createMinimumDelayAppliesTestData(NEGATIVE_CCO, 3);
    checkRequest("GET", "/debt-collection-enforcement/test-data/rep-orders-eligible-for-min-delay-applies-fdc?delayPeriod=5&dateReceived=01-JAN-2015&numRecords=3");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/create-fdc-contribution?repOrderId=1");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/insert-fdc-items?fdcId=1001");
    checkRequest("DELETE", "/debt-collection-enforcement/test-data/delete-crown-court-outcomes?repOrderId=1");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/create-fdc-contribution?repOrderId=2");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/insert-fdc-items?fdcId=2001");
    checkRequest("DELETE", "/debt-collection-enforcement/test-data/delete-crown-court-outcomes?repOrderId=2");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/create-fdc-contribution?repOrderId=3");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/insert-fdc-items?fdcId=3001");
    checkRequest("DELETE", "/debt-collection-enforcement/test-data/delete-crown-court-outcomes?repOrderId=3");
  }

  @Test
  void test_whenCreateMinimumDelayAppliesTestDataNegativeFdcStatus_thenShouldUpdateFdcContributionsToSent()
      throws InterruptedException {
    fdcTestDataCreatorService.createMinimumDelayAppliesTestData(NEGATIVE_FDC_STATUS, 1);
    checkRequest("GET", "/debt-collection-enforcement/test-data/rep-orders-eligible-for-min-delay-applies-fdc?delayPeriod=5&dateReceived=01-JAN-2015&numRecords=1");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/create-fdc-contribution?repOrderId=1");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/insert-fdc-items?fdcId=1001");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/update-fdc-contribution-to-sent?repOrderId=1&previousStatus=&newStatus=SENT");
  }

  @Test
  void test_whenCreateMinimumDelayAppliesTestDataNegativeFdcItem_thenShouldNotCreateFdcItems()
      throws InterruptedException {
    fdcTestDataCreatorService.createMinimumDelayAppliesTestData(NEGATIVE_FDC_ITEM, 1);
    checkRequest("GET", "/debt-collection-enforcement/test-data/rep-orders-eligible-for-min-delay-applies-fdc?delayPeriod=5&dateReceived=01-JAN-2015&numRecords=1");
    checkRequest("PUT", "/debt-collection-enforcement/test-data/create-fdc-contribution?repOrderId=1");
  }

  /**
   * Utility method to take the next request received by the mock web server and test it against the expected request
   * @param expectedMethod expected HTTP method type such as GET, PUT, POST or DELETE
   * @param expectedRequestPath expected full request path
   * @throws InterruptedException
   */
  private void checkRequest(String expectedMethod, String expectedRequestPath)
      throws InterruptedException {
    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    assertNotNull(recordedRequest);
    assertEquals(expectedMethod, recordedRequest.getMethod());
    assertEquals(expectedRequestPath, recordedRequest.getPath());
  }

  /**
   * Dispatcher to generate mock success responses for different types of requests
   */
  Dispatcher dispatcher = new Dispatcher() {
    @NotNull
    @Override
    public MockResponse dispatch(RecordedRequest request) {
      MockResponse mockResponse = new MockResponse().setResponseCode(HttpStatus.OK.value()).addHeader("Content-Type", "application/json");
      String requestPath = request.getPath();
      if (requestPath.contains("/rep-orders-eligible-for-min-delay-applies-fdc")) {
        mockResponse.setBody(getRequestedNumberOfMockRepIds(request));
      } else if (requestPath.contains("/create-fdc-contribution")) {
        int repOrderId = Integer.parseInt(request.getRequestUrl().queryParameter("repOrderId"));
        mockResponse.setBody(Integer.toString(repOrderId*1000+1));
      } else if (!(requestPath.contains("/insert-fdc-items") || //These are valid paths that don't return anything except status 200
          requestPath.contains("/update-fdc-contribution-to-sent") ||
          requestPath.contains("/delete-crown-court-outcomes") ||
          requestPath.contains("/update-rep-order-sentence-order-date"))){
        mockResponse.setResponseCode(HttpStatus.NOT_FOUND.value());
      }
      return mockResponse;
    }
  };

  /**
   *
   * @param request incoming request which contains request parameter numRecords
   * @return String representing a list of the requested number of mock rep IDs
   */
  private static String getRequestedNumberOfMockRepIds(RecordedRequest request) {
    int recordsToUpdate = Integer.parseInt(request.getRequestUrl().queryParameter("numRecords"));
    return IntStream.range(1, recordsToUpdate + 1)
        .boxed()
        .toList()
        .toString();
  }
}