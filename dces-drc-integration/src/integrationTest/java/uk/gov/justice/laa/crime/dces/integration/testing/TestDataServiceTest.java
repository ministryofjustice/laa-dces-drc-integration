package uk.gov.justice.laa.crime.dces.integration.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.SENT;
import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.WAITING_ITEMS;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_CCO;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_ITEM;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_STATUS;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_PREVIOUS_FDC;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_SOD;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.POSITIVE;

import java.time.LocalDate;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItemType;
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

  @MockitoBean
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


  // Fast Track FDC
  @Test
  void given_NothingFound_whenFdcFastTrackCalled_thenErrors(){
    when(maatApiClient.getFdcFastTrackRepOrderIdList(anyInt(),any(),anyInt())).thenReturn(null);
    Exception exception = assertThrows(RuntimeException.class, () ->
      testDataService.createFastTrackTestData(FdcAccelerationType.POSITIVE, POSITIVE, 5));
    assertEquals("No candidate rep orders found for fast-track pickup test type POSITIVE", exception.getMessage());
  }

  private void baseFdcFastTrackVerification(FdcAccelerationType accelerationType, Set<Long> idList, FdcTestType testType){
    when(maatApiClient.getFdcFastTrackRepOrderIdList(anyInt(),any(),anyInt())).thenReturn(idList);
    when(maatApiClient.updateRepOrder(any())).thenReturn(null);
    mockCreateFdcContribution();
    mockCreateFdcItem();

    Set<Long> returnedIds = testDataService.createFastTrackTestData(accelerationType, testType, 3);

    assertEquals(idList.size(), returnedIds.size());
    assertEquals(idList, returnedIds);
    verify(maatApiClient).getFdcFastTrackRepOrderIdList(anyInt(),any(),anyInt());
    int fdcItemCreationCount = (FdcAccelerationType.NEGATIVE.equals(accelerationType)?idList.size()*2:idList.size());
    verify(maatApiClient, times(fdcItemCreationCount)).createFdcItem(any());

    verify(maatApiClient, times(idList.size())).updateRepOrder(any());

    // check the base fdcContribution created on all paths.
    String expectedAccel = (FdcAccelerationType.POSITIVE.equals(accelerationType)?"Y":null);
    for(Long id: idList){
      verify(maatApiClient).createFdcContribution(new CreateFdcContributionRequest(id, "Y", "Y", expectedAccel, WAITING_ITEMS));
    }
  }

  @Test
  void whenPositiveCreateFastTrackPickupTestDataCalled_ThenShouldCallEndpoints(){
    baseFdcFastTrackVerification(FdcAccelerationType.POSITIVE, Set.of(1L,2L,3L,4L), POSITIVE);
  }

  @Test
  void whenPreviousCreateFastTrackPickupTestDataCalled_ThenShouldCallEndpoints(){
    Set<Long> idList = Set.of(1L,2L,3L,4L);
    baseFdcFastTrackVerification(FdcAccelerationType.PREVIOUS_FDC, idList, POSITIVE);
    for(Long id: idList){
      verify(maatApiClient).createFdcContribution(new CreateFdcContributionRequest(id, "Y", "Y", null, SENT));
    }
  }

  @Test
  void whenNegativeCreateFastTrackPickupTestDataCalled_ThenShouldCallEndpoints(){
    Set<Long> idList = Set.of(1L,2L,3L,4L);
    baseFdcFastTrackVerification(FdcAccelerationType.NEGATIVE, idList, POSITIVE);
    for(Long id: idList){
      FdcItem baseItem =     FdcItem.builder().fdcId(id).userCreated("DCES").dateCreated(LocalDate.now())
              .itemType(FdcItemType.LGFS).paidAsClaimed("Y").latestCostInd("Current").build();
      FdcItem negativeItem =     FdcItem.builder().fdcId(id).userCreated("DCES").dateCreated(LocalDate.now())
              .itemType(FdcItemType.AGFS).adjustmentReason("Pre AGFS Transfer").paidAsClaimed("N").latestCostInd("Current").build();

      verify(maatApiClient).createFdcItem(baseItem);
      verify(maatApiClient).createFdcItem(negativeItem);
    }
  }
  
  // Delayed FDC
  @Test
  void given_NothingFound_whenFdcDelayCalled_thenErrors(){
    when(maatApiClient.getFdcDelayedRepOrderIdList(anyInt(),any(),anyInt())).thenReturn(null);
    Exception exception = assertThrows(RuntimeException.class, () ->
            testDataService.createDelayedPickupTestData(POSITIVE, 5));
    assertEquals("No candidate rep orders found for delayed pickup test type POSITIVE", exception.getMessage());
  }

  // Method to do base checks and mocks for fast track calls.
  private void baseFdcDelayedVerification(Set<Long> idList, FdcTestType testType, int recordsToUpdate){
    when(maatApiClient.getFdcDelayedRepOrderIdList(anyInt(),any(),anyInt())).thenReturn(idList);
    mockCreateFdcContribution();
    mockCreateFdcItem();

    Set<Long> returnedIds = testDataService.createDelayedPickupTestData(testType, recordsToUpdate);

    assertEquals(idList.size(), returnedIds.size());
    assertEquals(idList, returnedIds);
    verify(maatApiClient).getFdcDelayedRepOrderIdList(anyInt(),any(),anyInt());
    verify(maatApiClient, times(idList.size())).createFdcItem(any());
    verify(maatApiClient, times(idList.size())).createFdcContribution(any());

  }

  @Test
  void whenPositiveCreateDelayedPickupTestDataCalled_ThenShouldCallEndpoints(){
    baseFdcDelayedVerification(Set.of(1L,2L,3L,4L), POSITIVE, 3);
  }

  @Test
  void whenNegativeSOD_DoBaseAndCallUpdateRepOrder(){
    Set<Long> idSet = Set.of(1L,2L,3L,4L);
    when(maatApiClient.updateRepOrderSentenceOrderDateToNull(anyLong(), any())).thenReturn(null);
    baseFdcDelayedVerification(idSet, NEGATIVE_SOD, 3);
    verify(maatApiClient, times(idSet.size())).updateRepOrderSentenceOrderDateToNull(anyLong(), any());
  }

  @Test
  void whenNegativeCCO_DoBaseAndCallUpdateRepOrder(){
    Set<Long> idSet = Set.of(1L,2L,3L,4L);
    when(maatApiClient.deleteCrownCourtOutcomes(anyLong())).thenReturn(null);
    baseFdcDelayedVerification(idSet, NEGATIVE_CCO, 3);
    verify(maatApiClient, times(idSet.size())).deleteCrownCourtOutcomes(anyLong());
  }

  @Test
  void whenNegativeFDCItem_DoBaseAndCallUpdateRepOrder(){
    Set<Long> idSet = Set.of(1L,2L,3L,4L);
    when(maatApiClient.deleteFdcItems(anyLong())).thenReturn(null);
    baseFdcDelayedVerification(idSet, NEGATIVE_FDC_ITEM, 3);
    verify(maatApiClient, times(idSet.size())).deleteFdcItems(anyLong());
  }

  @Test
  void whenNegativePreviousFDC_DoBaseAndCallUpdateRepOrder(){
    UpdateFdcContributionRequest expectedRequest = new UpdateFdcContributionRequest(1L,1L,FdcContributionsStatus.SENT.name(), FdcContributionsStatus.WAITING_ITEMS);
    when(maatApiClient.updateFdcContribution(expectedRequest)).thenReturn(null);
    baseFdcDelayedVerification(Set.of(1L), NEGATIVE_PREVIOUS_FDC, 3);
    verify(maatApiClient, times(1)).updateFdcContribution(expectedRequest);
  }

  @Test
  void whenNegativeFdcStatus_DoBaseAndCallUpdateRepOrder(){
    UpdateFdcContributionRequest expectedRequest = new UpdateFdcContributionRequest(1L,1L,null, FdcContributionsStatus.SENT);
    when(maatApiClient.updateFdcContribution(expectedRequest)).thenReturn(null);
    baseFdcDelayedVerification(Set.of(1L), NEGATIVE_FDC_STATUS, 3);
    verify(maatApiClient, times(1)).updateFdcContribution(expectedRequest);
  }

}