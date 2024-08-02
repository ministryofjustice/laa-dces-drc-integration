package uk.gov.justice.laa.crime.dces.integration.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_CCO;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_ITEM;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_STATUS;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_PREVIOUS_FDC;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_SOD;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.POSITIVE;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;
import uk.gov.justice.laa.crime.dces.integration.service.TestDataService;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;


@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDataServiceTest {

  TestDataService testDataService;

  @MockBean
  MaatApiClient maatApiClient;

  @BeforeAll
  public void setup() {
    testDataService = new TestDataService(maatApiClient);
  }

  private void mockCreateFdcContribution(){
    when(maatApiClient.createFdcContribution(any())).thenAnswer(inv -> {
              CreateFdcContributionRequest request = (CreateFdcContributionRequest) inv.getArguments()[0];
              return FdcContribution.builder()
                      .id(request.getRepId())
                      .agfsComplete(request.getAgfsComplete())
                      .lgfsComplete(request.getLgfsComplete())
                      .accelerate(request.getManualAcceleration())
                      .status(request.getStatus())
                      .maatId(request.getRepId())
                      .build();
            }
    );
  }

  private void mockCreateFdcItem(){
    when(maatApiClient.createFdcItem(any())).thenReturn(new FdcItem());
  }

  private void baseFdcDelayedVerification(Set<Integer> idList, FdcTestType testType, int recordsToUpdate){
    when(maatApiClient.getFdcDelayedRepOrderIdList(anyInt(),any(),anyInt())).thenReturn(idList);
    mockCreateFdcContribution();
    mockCreateFdcItem();

    Set<Integer> returnedIds = testDataService.createDelayedPickupTestData(testType, recordsToUpdate);

    assertEquals(idList.size(), returnedIds.size());
    assertEquals(idList, returnedIds);
    verify(maatApiClient).getFdcDelayedRepOrderIdList(anyInt(),any(),anyInt());
    verify(maatApiClient, times(idList.size())).createFdcItem(any());
    verify(maatApiClient, times(idList.size())).createFdcContribution(any());

  }

  //   case NEGATIVE_SOD -> maatApiClient.updateRepOrder(UpdateRepOrder.builder().repId(repOrderId)
  //          .sentenceOrderDate(LocalDate.now().plusMonths(monthsAfterSysDate)).build());
  //      case NEGATIVE_CCO -> maatApiClient.deleteCrownCourtOutcomes(repOrderId);
  //      case NEGATIVE_FDC_ITEM -> maatApiClient.deleteFdcItem(fdcId);
  //      case NEGATIVE_PREVIOUS_FDC -> maatApiClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, SENT.name(), WAITING_ITEMS));
  //      case NEGATIVE_FDC_STATUS -> maatApiClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, null, SENT));
  //

  @Test
  void whenPositiveCreateDelayedPickupTestDataCalled_ThenShouldCallEndpoints(){
    baseFdcDelayedVerification(Set.of(1,2,3,4), POSITIVE, 3);
  }

  @Test
  void whenNegativeSOD_DoBaseAndCallUpdateRepOrder(){
    Set<Integer> idSet = Set.of(1,2,3,4);
    when(maatApiClient.updateRepOrder(any())).thenReturn(null);
    baseFdcDelayedVerification(idSet, NEGATIVE_SOD, 3);
    verify(maatApiClient, times(idSet.size())).updateRepOrder(any());
  }

  @Test
  void whenNegativeCCO_DoBaseAndCallUpdateRepOrder(){
    Set<Integer> idSet = Set.of(1,2,3,4);
    when(maatApiClient.deleteCrownCourtOutcomes(anyInt())).thenReturn(null);
    baseFdcDelayedVerification(idSet, NEGATIVE_CCO, 3);
    verify(maatApiClient, times(idSet.size())).deleteCrownCourtOutcomes(anyInt());
  }

  @Test
  void whenNegativeFDCItem_DoBaseAndCallUpdateRepOrder(){
    Set<Integer> idSet = Set.of(1,2,3,4);
    when(maatApiClient.deleteFdcItem(anyInt())).thenReturn(null);
    baseFdcDelayedVerification(idSet, NEGATIVE_FDC_ITEM, 3);
    verify(maatApiClient, times(idSet.size())).deleteFdcItem(anyInt());
  }

  //case NEGATIVE_PREVIOUS_FDC -> maatApiClient.updateFdcContribution(
  // new UpdateFdcContributionRequest(fdcId, repOrderId, SENT.name(), WAITING_ITEMS));
  @Test
  void whenNegativePreviousFDC_DoBaseAndCallUpdateRepOrder(){
    UpdateFdcContributionRequest expectedRequest = new UpdateFdcContributionRequest(1,1,FdcContributionsStatus.SENT.name(), FdcContributionsStatus.WAITING_ITEMS);
    when(maatApiClient.updateFdcContribution(expectedRequest)).thenReturn(null);
    baseFdcDelayedVerification(Set.of(1), NEGATIVE_PREVIOUS_FDC, 3);
    verify(maatApiClient, times(1)).updateFdcContribution(expectedRequest);
  }

  // case NEGATIVE_FDC_STATUS -> maatApiClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, null, SENT));
  @Test
  void whenNegativeFdcStatus_DoBaseAndCallUpdateRepOrder(){
    UpdateFdcContributionRequest expectedRequest = new UpdateFdcContributionRequest(1,1,null, FdcContributionsStatus.SENT);
    when(maatApiClient.updateFdcContribution(expectedRequest)).thenReturn(null);
    baseFdcDelayedVerification(Set.of(1), NEGATIVE_FDC_STATUS, 3);
    verify(maatApiClient, times(1)).updateFdcContribution(expectedRequest);
  }

//
//  @Test
//  void test_whenCreateDelayedPickupTestDataNegativeSod_thenShouldUpdateSod()
//      throws InterruptedException {
//
//    int recordsToUpdate = 3;
//    LocalDate date = LocalDate.of(2015,1,1);
//
//    Set<Integer> refIds = testDataService.createDelayedPickupTestData(NEGATIVE_SOD, 3);
//    assertEquals(3, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=5&dateReceived=2015-01-01&numRecords=3&fdcDelayedPickup=true&fdcFastTrack=false");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":2,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1002,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":2,\"sentenceOrderDate\":\""+getDateAfterMonths(3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":3,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1003,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":3,\"sentenceOrderDate\":\""+getDateAfterMonths(3)+"\"}");
//  }
//
//  @Test
//  void test_whenCreateDelayedPickupTestDataNegativeCco_thenShouldDeleteCco()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createDelayedPickupTestData(NEGATIVE_CCO, 3);
//    assertEquals(3, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=5&dateReceived=2015-01-01&numRecords=3&fdcDelayedPickup=true&fdcFastTrack=false");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequest("DELETE", "/assessment/rep-orders/cc-outcome/rep-order/1");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":2,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1002,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequest("DELETE", "/assessment/rep-orders/cc-outcome/rep-order/2");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":3,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1003,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequest("DELETE", "/assessment/rep-orders/cc-outcome/rep-order/3");
//  }
//
//  @Test
//  void test_whenCreateDelayedPickupTestDataNegativeFdcStatus_thenShouldUpdateFdcContributionToSent()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createDelayedPickupTestData(NEGATIVE_FDC_STATUS, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=5&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=true&fdcFastTrack=false");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequestAndBody("PATCH", "/debt-collection-enforcement/fdc-contribution",
//        "{\"fdcContributionId\":1001,\"repId\":1,\"previousStatus\":null,\"newStatus\":\"SENT\"}");
//  }
//
//  @Test
//  void test_whenCreateDelayedPickupTestDataNegativeFdcItem_thenShouldDeleteFdcItems()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createDelayedPickupTestData(NEGATIVE_FDC_ITEM, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=5&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=true&fdcFastTrack=false");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequest("DELETE", "/debt-collection-enforcement/fdc-items/fdc-id/1001");
//  }
//
//  @Test
//  void test_whenCreateFastTrackTestDataPositiveAcceleration_thenShouldCreateContributionAndFdcItem()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  POSITIVE, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//  }
//
//  @Test
//  void test_whenCreateFastTrackTestDataPositiveAccelerationNegSod_thenShouldDoSevenMonths()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  NEGATIVE_SOD, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-7)+"\"}");
//  }
//
//  @Test
//  void test_whenCreateFastTrackTestDataPositiveAccelerationNegCco_thenShouldDeleteCco()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  NEGATIVE_CCO, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequest("DELETE", "/assessment/rep-orders/cc-outcome/rep-order/1");
//  }
//
//  @Test
//  void test_whenCreateFastTrackTestDataPositiveAccelerationNegFdcStatus_thenShouldUpdateFdcContributionsToSent()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  NEGATIVE_FDC_STATUS, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequestAndBody("PATCH", "/debt-collection-enforcement/fdc-contribution",
//        "{\"fdcContributionId\":1001,\"repId\":1,\"previousStatus\":null,\"newStatus\":\"SENT\"}");
//  }
//
//  @Test
//  void test_whenCreateFastTrackTestDataPositiveAccelerationNegFdcItem_thenShouldDeleteFdcItem()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createFastTrackTestData(FdcAccelerationType.POSITIVE,  NEGATIVE_FDC_ITEM, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"manualAcceleration\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequest("DELETE", "/debt-collection-enforcement/fdc-items/fdc-id/1001");
//  }
//
//  @Test
//  void test_whenCreateFastTrackTestDataNegativeAcceleration_thenShouldCreateTwoFdcItems()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createFastTrackTestData(FdcAccelerationType.NEGATIVE,  POSITIVE, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"itemType\":\"LGFS\",\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"paidAsClaimed\":\"Y\",\"latestCostInd\":\"Current\",\"userCreated\":\"DCES\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"itemType\":\"AGFS\",\"adjustmentReason\":\"Pre AGFS Transfer\",\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"paidAsClaimed\":\"N\",\"latestCostInd\":\"Current\",\"userCreated\":\"DCES\"}");
//  }
//
//  @Test
//  void test_whenCreateFastTrackTestDataPrevFdc_thenShouldCreateTwoContributions()
//      throws InterruptedException {
//    Set<Integer> refIds = testDataService.createFastTrackTestData(FdcAccelerationType.PREVIOUS_FDC,  POSITIVE, 1);
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"SENT\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//  }
//
//  @Test
//  void test_whenCreateFastTrackTestDataPrevFdcNegative_thenShouldCreateTwoWaitingContributions()
//      throws InterruptedException {
//    Set<Integer> refIds = null;
//    try{
//      refIds = testDataService.createFastTrackTestData(FdcAccelerationType.PREVIOUS_FDC, NEGATIVE_PREVIOUS_FDC, 1);
//    }
//    catch (NullPointerException e){
//      log.info("We erroring?");
//    }
//
//
//    assertEquals(1, refIds.size());
//    checkRequest("GET", "/assessment/rep-orders?delay=-3&dateReceived=2015-01-01&numRecords=1&fdcDelayedPickup=false&fdcFastTrack=true");
//    checkRequestAndBody("PUT", "/assessment/rep-orders", "{\"repId\":1,\"sentenceOrderDate\":\""+getDateAfterMonths(-3)+"\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"WAITING_ITEMS\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-contribution",
//        "{\"repId\":1,\"lgfsComplete\":\"Y\",\"agfsComplete\":\"Y\",\"status\":\"SENT\"}");
//    checkRequestAndBody("POST", "/debt-collection-enforcement/fdc-items",
//        "{\"fdcId\":1001,\"dateCreated\":\""+getDateAfterMonths(0)+"T00:00:00.000\",\"userCreated\":\"DCES\"}");
//    checkRequestAndBody("PATCH", "/debt-collection-enforcement/fdc-contribution",
//        "{\"fdcContributionId\":1001,\"repId\":1,\"previousStatus\":\"SENT\",\"newStatus\":\"WAITING_ITEMS\"}");
//  }
//
//  /**
//   * Utility method to take the next request received by the mock web server and test it against the expected request
//   * @param expectedMethod expected HTTP method type such as GET, PUT, POST or DELETE
//   * @param expectedRequestPath expected full request path
//   * @throws InterruptedException Thrown by mockWebServer.takeRequest
//   */
//  private RecordedRequest checkRequest(String expectedMethod, String expectedRequestPath)
//      throws InterruptedException {
////    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
////    assertNotNull(recordedRequest);
////    assertEquals(expectedMethod, recordedRequest.getMethod());
////    assertEquals(expectedRequestPath, recordedRequest.getPath());
////    return recordedRequest;
//  }
//
//  /**
//   * Utility method to check that no more requests are received by the mock server (to ensure that the next test can proceed)
//   * @throws InterruptedException Thrown by mockWebServer.takeRequest
//   */
//  @AfterEach
//  public void checkNullRequest()
//      throws InterruptedException {
////    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
////    assertNull(recordedRequest);
//  }
//
//  /**
//   * Utility method to take the next request received by the mock web server and test it against the expected request
//   * @param expectedMethod expected HTTP method type such as GET, PUT, POST or DELETE
//   * @param expectedRequestPath expected full request path
//   * @throws InterruptedException Thrown by checkRequest
//   */
//  private void checkRequestAndBody(String expectedMethod, String expectedRequestPath, String expectedRequestBody)
//      throws InterruptedException {
////    RecordedRequest recordedRequest = checkRequest(expectedMethod, expectedRequestPath);
////    assertEquals(expectedRequestBody, recordedRequest.getBody().readUtf8());
//  }

}