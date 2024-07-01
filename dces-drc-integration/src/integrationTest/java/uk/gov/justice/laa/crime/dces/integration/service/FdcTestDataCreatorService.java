package uk.gov.justice.laa.crime.dces.integration.service;

import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcContributionsStatus.SENT;
import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcContributionsStatus.WAITING_ITEMS;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_ITEM;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_STATUS;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_PREVIOUS_FDC;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContributionsStatus;
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

  public void createMinimumDelayAppliesTestData(FdcTestType testType, int recordsToUpdate){
    List<Integer> repOrderIds = testDataClient.getRepOrdersEligibleForMinDelayAppliesFDCs(5, "01-JAN-2015", recordsToUpdate);
    FdcContributionsStatus contributionStatus = (testType==NEGATIVE_FDC_STATUS)?SENT:WAITING_ITEMS;
    repOrderIds.forEach(repOrderId -> {
      int fdcId = testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId,"Y", "Y", null, contributionStatus));
      processNegativeTests(testType, repOrderId, FdcItem.builder().fdcId(fdcId).userCreated("DCES").build(), 3);
    });
  }

  public void createMinimumDelayNotAppliesTestData( FdcAccelerationType fdcAccelerationType, FdcTestType testType, int recordsToUpdate){
    List<Integer> repOrderIds = testDataClient.getRepOrdersEligibleForMinDelayNotAppliesFDCs(-3, "01-JAN-2015", recordsToUpdate);
    repOrderIds.forEach(repOrderId -> {
      testDataClient.updateRepOrderSentenceOrderDate(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().plusMonths(-3)).build());
      FdcContributionsStatus contributionStatus = (testType==NEGATIVE_FDC_STATUS)?SENT:WAITING_ITEMS;
      String manualAcceleration = (fdcAccelerationType == FdcAccelerationType.POSITIVE)?"Y":null;
      int fdcId = testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", manualAcceleration, contributionStatus));
      if (fdcAccelerationType.equals(FdcAccelerationType.PREVIOUS_FDC)) {
        contributionStatus = (testType==NEGATIVE_PREVIOUS_FDC)?WAITING_ITEMS:SENT;
        testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", null, contributionStatus));
      }
      FdcItemBuilder fdcItemBuilder = FdcItem.builder().fdcId(fdcId).userCreated("DCES");
      if (fdcAccelerationType.equals(FdcAccelerationType.NEGATIVE)) {
        fdcItemBuilder.itemType(FdcItemType.LGFS).paidAsClaimed("Y").latestCostInd("Current");
        testDataClient.createFdcItems(fdcItemBuilder.build());
        fdcItemBuilder.itemType(FdcItemType.AGFS).adjustmentReason("Pre AGFS Transfer").paidAsClaimed("N").latestCostInd("Current");
      }
      processNegativeTests(testType, repOrderId, fdcItemBuilder.build(), -7);
    });
  }

  private void processNegativeTests(FdcTestType testType, Integer repOrderId, FdcItem fdcItem, int monthsAfterSysDate) {
    if (testType != NEGATIVE_FDC_ITEM) {
      testDataClient.createFdcItems(fdcItem); //.getFdcId(), fdcItem.getItemType().toString(),
          //fdcItem.getAdjustmentReason(), fdcItem.getPaidAsClaimed(), fdcItem.getLatestCostInd(), fdcItem.getUserCreated());
    }
    switch (testType) {
      case NEGATIVE_SOD ->
          testDataClient.updateRepOrderSentenceOrderDate(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().plusMonths(monthsAfterSysDate)).build());
      case NEGATIVE_CCO ->
          testDataClient.deleteCrownCourtOutcomes(repOrderId);
    }
  }

}
