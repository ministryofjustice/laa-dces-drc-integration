package uk.gov.justice.laa.crime.dces.integration.service;

import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_ITEM;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_FDC_STATUS;
import static uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType.NEGATIVE_PREVIOUS_FDC;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateRepOrder;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItem.FdcItemBuilder;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItemType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;

@Slf4j
@Service
@RequiredArgsConstructor
public class FdcTestDataCreatorService {

  private final TestDataClient testDataClient;

  public void createMinimumDelayAppliesTestData(FdcTestType testType, int recordsToUpdate){
    List<Integer> repOrderIds = testDataClient.getRepOrdersEligibleForMinDelayAppliesFDCs(5, "01-JAN-2015", recordsToUpdate);
    String contributionStatus = (testType==NEGATIVE_FDC_STATUS)?"SENT":"WAITING_ITEMS";
    repOrderIds.forEach(repOrderId -> {
      int fdcId = testDataClient.createFdcContribution(new FdcContribution(repOrderId,"Y", "Y", contributionStatus));
      processNegativeTests(testType, repOrderId, FdcItem.builder().fdcId(fdcId).itemType(FdcItemType.NULL).adjustmentReason("").paidAsClaimed("").latestCostIndicator("").userCreated("DCES").build(), 3);
    });
  }

  public void createMinimumDelayNotAppliesTestData( FdcAccelerationType fdcAccelerationType, FdcTestType testType, int recordsToUpdate){
    List<Integer> repOrderIds = testDataClient.getRepOrdersEligibleForMinDelayNotAppliesFDCs(-3, "01-JAN-2015", recordsToUpdate);
    repOrderIds.forEach(repOrderId -> {
      testDataClient.updateRepOrderSentenceOrderDate(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().plusMonths(-3)).build());
      String contributionStatus = (testType==NEGATIVE_FDC_STATUS)?"SENT":"WAITING_ITEMS";
      int fdcId = testDataClient.createFdcContribution(new FdcContribution(repOrderId, "Y", "Y", contributionStatus));
      if (fdcAccelerationType.equals(FdcAccelerationType.PREVIOUS_FDC)) {
        contributionStatus = (testType==NEGATIVE_PREVIOUS_FDC)?"WAITING_ITEMS":"SENT";
        testDataClient.createFdcContribution(new FdcContribution(repOrderId, "Y", "Y", contributionStatus));
      }
      FdcItemBuilder fdcItemBuilder = FdcItem.builder().fdcId(fdcId).userCreated("DCES").itemType(FdcItemType.NULL).adjustmentReason("").paidAsClaimed("").latestCostIndicator("");
      if (fdcAccelerationType.equals(FdcAccelerationType.NEGATIVE)) {
        testDataClient.insertFdcItems(fdcId, FdcItemType.LGFS.name(), "", "Y", "Current", "DCES");
        fdcItemBuilder.itemType(FdcItemType.AGFS).adjustmentReason("Pre AGFS Transfer").paidAsClaimed("N").latestCostIndicator("Current");
      }
      processNegativeTests(testType, repOrderId, fdcItemBuilder.build(), -7);
    });
  }

  private void processNegativeTests(FdcTestType testType, Integer repOrderId, FdcItem fdcItem, int monthsAfterSysDate) {
    if (testType != NEGATIVE_FDC_ITEM) {
      testDataClient.insertFdcItems(fdcItem.getFdcId(), fdcItem.getItemType().toString(),
          fdcItem.getAdjustmentReason(), fdcItem.getPaidAsClaimed(), fdcItem.getLatestCostIndicator(), fdcItem.getUserCreated());
    }
    switch (testType) {
      case NEGATIVE_SOD ->
          testDataClient.updateRepOrderSentenceOrderDate(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().plusMonths(monthsAfterSysDate)).build());
      case NEGATIVE_CCO ->
          testDataClient.deleteCrownCourtOutcomes(repOrderId);
    }
  }

}
