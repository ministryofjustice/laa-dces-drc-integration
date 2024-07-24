package uk.gov.justice.laa.crime.dces.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_CCO;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_ITEM;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_STATUS;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_PREVIOUS_FDC;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_SOD;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.POSITIVE;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiWebClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;

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

  @MockBean
  MaatApiClient maatApiClient;

  @BeforeAll
  public void setup() {
    mockWebServer = new MockWebServer();
    mockWebServer.setDispatcher(dispatcher);
    configuration.getMaatApi().setBaseUrl(String.format("http://localhost:%s", mockWebServer.getPort()));
    TestDataClient newTestDataClient = MaatApiClientFactory.maatApiClient(
        (new MaatApiWebClientFactory()).maatApiWebClient(configuration, authorizedClientManager),
        TestDataClient.class);
    fdcTestDataCreatorService = new FdcTestDataCreatorService(newTestDataClient,maatApiClient);
  }

  @AfterAll
  void shutDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void test_whenCreateDelayedPickupTestDataNegativeSod_thenShouldUpdateSod()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createDelayedPickupTestData(NEGATIVE_SOD, 3);
    assertEquals(3, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=5&dateReceived=2015-01-01&numRecords=3&fdcDelayedPickup=true&fdcFastTrack=false");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");

    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":2,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1002,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":2,\"sentenceOrderDate\":\""+getDateAfterMonths(3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":3,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1003,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":3,\"sentenceOrderDate\":\""+getDateAfterMonths(3)+"\"}");
  }

  @Test
  void test_whenCreateDelayedPickupTestDataNegativeCco_thenShouldDeleteCco()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createDelayedPickupTestData(NEGATIVE_CCO, 3);
    assertEquals(3, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=5&dateReceived=2015-01-01&numRecords=3&fdcDelayedPickup=true&fdcFastTrack=false");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequest("DELETE", "/assessment/rep-orders/cc-outcome/rep-order/1");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":2,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1002,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequest("DELETE", "/assessment/rep-orders/cc-outcome/rep-order/2");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":3,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1003,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequest("DELETE", "/assessment/rep-orders/cc-outcome/rep-order/3");
  }

  @Test
  void test_whenCreateDelayedPickupTestDataNegativeFdcStatus_thenShouldUpdateFdcContributionToSent()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createDelayedPickupTestData(NEGATIVE_FDC_STATUS, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=5&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=true&fdcFastTrack=false");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequestAndBody("PATCH", "/debt-collection-enforcement/fdc-contribution",
        "{\"fdcContributionId\":1001,\"repId\":1,\"previousStatus\":null,\"newStatus\":\"SENT\"}");
  }

  @Test
  void test_whenCreateDelayedPickupTestDataNegativeFdcItem_thenShouldDeleteFdcItems()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createDelayedPickupTestData(NEGATIVE_FDC_ITEM, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=5&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=true&fdcFastTrack=false");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequest("DELETE", "/debt-collection-enforcement/fdc-items/fdc-id/1001");
  }

  @Test
  void test_whenCreateFastTrackTestDataPositiveAcceleration_thenShouldCreateContributionAndFdcItem()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  POSITIVE, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
  }

  @Test
  void test_whenCreateFastTrackTestDataPositiveAccelerationNegSod_thenShouldDoSevenMonths()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  NEGATIVE_SOD, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-7)+"\"}");
  }

  @Test
  void test_whenCreateFastTrackTestDataPositiveAccelerationNegCco_thenShouldDeleteCco()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  NEGATIVE_CCO, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequest("DELETE", "/assessment/rep-orders/cc-outcome/rep-order/1");
  }

  @Test
  void test_whenCreateFastTrackTestDataPositiveAccelerationNegFdcStatus_thenShouldUpdateFdcContributionsToSent()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  NEGATIVE_FDC_STATUS, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequestAndBody("PATCH", "/debt-collection-enforcement/fdc-contribution",
        "{\"fdcContributionId\":1001,\"repId\":1,\"previousStatus\":null,\"newStatus\":\"SENT\"}");
  }

  @Test
  void test_whenCreateFastTrackTestDataPositiveAccelerationNegFdcItem_thenShouldDeleteFdcItem()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  NEGATIVE_FDC_ITEM, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequest("DELETE", "/debt-collection-enforcement/fdc-items/fdc-id/1001");
  }

  @Test
  void test_whenCreateFastTrackTestDataNegativeAcceleration_thenShouldCreateTwoFdcItems()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createFastTrackTestData(FdcAccelerationType.NEGATIVE,  POSITIVE, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"itemType\":\"LGFS\",\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"paidAsClaimed\":\"Y\",\"latestCostInd\":\"Current\",\"userCreated\":\"DCES\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"itemType\":\"AGFS\",\"adjustmentReason\":\"Pre AGFS Transfer\",\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"paidAsClaimed\":\"N\",\"latestCostInd\":\"Current\",\"userCreated\":\"DCES\"}");
  }

  @Test
  void test_whenCreateFastTrackTestDataPrevFdc_thenShouldCreateTwoContributions()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createFastTrackTestData(FdcAccelerationType.PREVIOUS_FDC,  POSITIVE, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"SENT\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
  }

  @Test
  void test_whenCreateFastTrackTestDataPrevFdcNegative_thenShouldCreateTwoWaitingContributions()
      throws InterruptedException {
    Set<Integer> refIds = fdcTestDataCreatorService.createFastTrackTestData(FdcAccelerationType.PREVIOUS_FDC, NEGATIVE_PREVIOUS_FDC, 1);
    assertEquals(1, refIds.size());
    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"SENT\"}");
    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
    checkRequestAndBody("PATCH", "/debt-collection-enforcement/fdc-contribution",
        "{\"fdcContributionId\":1001,\"repId\":1,\"previousStatus\":\"SENT\",\"newStatus\":\"WAITING_ITEMS\"}");
  }

  /**
   * Utility method to take the next request received by the mock web server and test it against the expected request
   * @param expectedMethod expected HTTP method type such as GET, PUT, POST or DELETE
   * @param expectedRequestPath expected full request path
   * @throws InterruptedException Thrown by mockWebServer.takeRequest
   */
  private RecordedRequest checkRequest(String expectedMethod, String expectedRequestPath)
      throws InterruptedException {
    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    assertNotNull(recordedRequest);
    assertEquals(expectedMethod, recordedRequest.getMethod());
    assertEquals(expectedRequestPath, recordedRequest.getPath());
    return recordedRequest;
  }

  /**
   * Utility method to check that no more requests are received by the mock server (to ensure that the next test can proceed)
   * @throws InterruptedException Thrown by mockWebServer.takeRequest
   */
  @AfterEach
  public void checkNullRequest()
      throws InterruptedException {
    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    assertNull(recordedRequest);
  }

  /**
   * Utility method to take the next request received by the mock web server and test it against the expected request
   * @param expectedMethod expected HTTP method type such as GET, PUT, POST or DELETE
   * @param expectedRequestPath expected full request path
   * @throws InterruptedException Thrown by checkRequest
   */
  private void checkRequestAndBody(String expectedMethod, String expectedRequestPath, String expectedRequestBody)
      throws InterruptedException {
    RecordedRequest recordedRequest = checkRequest(expectedMethod, expectedRequestPath);
    assertEquals(expectedRequestBody, recordedRequest.getBody().readUtf8());
  }

  /**
   * Dispatcher to generate mock success responses for different types of requests
   */
  Dispatcher dispatcher = new Dispatcher() {

    private int mockFdcId = 1001;

    public void resetMockFdcId() {mockFdcId = 1001;}
    @SneakyThrows
    @NotNull
    @Override
    public MockResponse dispatch(RecordedRequest request) {
      MockResponse mockResponse = new MockResponse().setResponseCode(HttpStatus.OK.value()).addHeader("Content-Type", "application/json");
      String requestPath = request.getPath();
      if (requestPath != null) {
        if (requestPath.contains("/fdc-contribution")) {
          if ("POST".equals(request.getMethod())) {
            mockResponse.setBody("{\"id\" : \""+mockFdcId++ +"\"}");
          }
          else if ("PATCH".equals(request.getMethod())) mockResponse.setBody("1");
        } else if (requestPath.contains("/assessment/rep-orders/cc-outcome/rep-order") && "DELETE".equals(request.getMethod())) {
          mockResponse.setBody("1");
        } else if ("GET".equals(request.getMethod()) && requestPath.contains("/assessment/rep-orders")) {
          mockResponse.setBody(getRequestedNumberOfMockRepIds(request));
          mockFdcId = 1001;
        } else if (!validRequestPath(requestPath)) {
          mockResponse.setResponseCode(HttpStatus.NOT_FOUND.value());
        }
      }
      return mockResponse;
    }
  };

  /**
   * Checks if the request path is a valid, known path
   * @param requestPath request path to be checked for validity
   * @return boolean indicating whether the request path is valid or not
   */
  private boolean validRequestPath(String requestPath) {
    List<String> validPaths = Arrays.asList("/fdc-items", "/assessment/rep-orders");
    return validPaths.stream().anyMatch(requestPath::contains);
  }

  /**
   *
   * @param request incoming request which contains request parameter numRecords
   * @return String representing a list of the requested number of mock rep IDs
   */
  private static String getRequestedNumberOfMockRepIds(RecordedRequest request) {
    int recordsToUpdate = 1;
    if (request.getRequestUrl() != null && request.getRequestUrl().queryParameter("numRecords") != null) {
      recordsToUpdate = Integer.parseInt(request.getRequestUrl().queryParameter("numRecords"));
    }
    return IntStream.range(1, recordsToUpdate + 1)
        .boxed()
        .toList()
        .toString();
  }

  /**
   *
   * @param monthsToAdd Number of months to add to system date
   * @return String representing the result of adding the months to current system date
   */
  private String getDateAfterMonths(int monthsToAdd) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return dateTimeFormatter.format(LocalDate.now().plusMonths(monthsToAdd));
  }


}