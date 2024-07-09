package uk.gov.justice.laa.crime.dces.integration.service;

import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcContributionsStatus.SENT;
import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcContributionsStatus.WAITING_ITEMS;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateRepOrder;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem.FdcItemBuilder;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItemType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;

@Slf4j
@Service
@RequiredArgsConstructor
public class FdcTestDataCreatorService {

  private final TestDataClient testDataClient;

  public void createDelayedPickupTestData(FdcTestType testType, int recordsToUpdate){
    List<Integer> repOrderIds = testDataClient.getRepOrders(5, "2015-01-01", recordsToUpdate, true, false);
    if (repOrderIds != null && !repOrderIds.isEmpty()) {
      repOrderIds.forEach(repOrderId -> {
        FdcContribution fdcContribution = testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", null, WAITING_ITEMS));
        int fdcId = fdcContribution.getId();
        testDataClient.createFdcItems(FdcItem.builder().fdcId(fdcId).userCreated("DCES").build());
        processNegativeTests(testType, repOrderId, fdcId, 3);
      });
    } else {
      throw new RuntimeException("No candidate rep orders found for delayed pickup test type " + testType);
    }
  }

  public void createFastTrackTestData( FdcAccelerationType fdcAccelerationType, FdcTestType testType, int recordsToUpdate){
    List<Integer> repOrderIds = testDataClient.getRepOrders(-3, "2015-01-01", recordsToUpdate, false, true);
    if (repOrderIds != null && !repOrderIds.isEmpty()) {
      repOrderIds.forEach(repOrderId -> {
        testDataClient.updateRepOrderSentenceOrderDate(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().plusMonths(-3)).build());
        String manualAcceleration = (fdcAccelerationType == FdcAccelerationType.POSITIVE)?"Y":null;
        int fdcId = testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", manualAcceleration, WAITING_ITEMS)).getId();
        if (fdcAccelerationType.equals(FdcAccelerationType.PREVIOUS_FDC)) {
          testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", null, SENT));
        }
        FdcItemBuilder fdcItemBuilder = FdcItem.builder().fdcId(fdcId).userCreated("DCES");
        if (fdcAccelerationType.equals(FdcAccelerationType.NEGATIVE)) {
          fdcItemBuilder.itemType(FdcItemType.LGFS).paidAsClaimed("Y").latestCostInd("Current");
          testDataClient.createFdcItems(fdcItemBuilder.build());
          fdcItemBuilder.itemType(FdcItemType.AGFS).adjustmentReason("Pre AGFS Transfer").paidAsClaimed("N").latestCostInd("Current");
        }
        testDataClient.createFdcItems(fdcItemBuilder.build());
        processNegativeTests(testType, repOrderId, fdcId, -7);
      });
    } else {
      throw new RuntimeException("No candidate rep orders found for delayed pickup test type " + testType);
    }
  }

  private void processNegativeTests(FdcTestType testType, Integer repOrderId, Integer fdcId, int monthsAfterSysDate) {
    switch (testType) {
      case NEGATIVE_SOD -> testDataClient.updateRepOrderSentenceOrderDate(UpdateRepOrder.builder().repId(repOrderId)
          .sentenceOrderDate(LocalDate.now().plusMonths(monthsAfterSysDate)).build());
      case NEGATIVE_CCO -> testDataClient.deleteCrownCourtOutcomes(repOrderId);
      case NEGATIVE_FDC_ITEM -> testDataClient.deleteFdcItems(fdcId);
      case NEGATIVE_PREVIOUS_FDC -> testDataClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, SENT.name(), WAITING_ITEMS));
      case NEGATIVE_FDC_STATUS -> testDataClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, null, SENT));
    }
  }

}
