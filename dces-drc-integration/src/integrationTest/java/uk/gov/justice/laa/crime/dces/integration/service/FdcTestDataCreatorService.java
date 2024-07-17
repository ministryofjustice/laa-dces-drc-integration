package uk.gov.justice.laa.crime.dces.integration.service;

import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.SENT;
import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.WAITING_ITEMS;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
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

  /**
   * Create test data required for testing the FDC Delayed Pickup logic
   * (	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-365</a> for test specification.)
   * @param testType: One of the test types defined in enum FdcTestType
   * @param recordsToUpdate Number of records to update
   * @return A set of FDC IDs updated as required
   */
  public Set<Integer> createDelayedPickupTestData(FdcTestType testType, int recordsToUpdate){
    Set<Integer> repOrderIds = testDataClient.getRepOrders(5, "2015-01-01", recordsToUpdate, true, false);
    Set<Integer> fdcIds = new HashSet<>();
    if (repOrderIds != null && !repOrderIds.isEmpty()) {
      repOrderIds.forEach(repOrderId -> {
        FdcContribution fdcContribution = testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", null, WAITING_ITEMS));
        int fdcId = fdcContribution.getId();
        fdcIds.add(fdcId);
        testDataClient.createFdcItems(FdcItem.builder().fdcId(fdcId).userCreated("DCES").dateCreated(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)).build());
        processNegativeTests(testType, repOrderId, fdcId, 3);
      });
    } else {
      throw new RuntimeException("No candidate rep orders found for delayed pickup test type " + testType);
    }
    return fdcIds;
  }

  /**
   * Create test data required for testing the FDC Fast Track  logic
   * (	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-367</a>,
   * <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-377</a> and
   * <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-378</a>for test specification (this one method caters to all of them).)
   * @param fdcAccelerationType: One of the acceleration types defined in enum FdcAccelerationType
   * @param testType: One of the test types defined in enum FdcTestType
   * @param recordsToUpdate Number of records to update
   * @return A set of FDC IDs updated as required
   */

  public Set<Integer> createFastTrackTestData( FdcAccelerationType fdcAccelerationType, FdcTestType testType, int recordsToUpdate){
    Set<Integer> repOrderIds = testDataClient.getRepOrders(-3, "2015-01-01", recordsToUpdate, false, true);
    Set<Integer> fdcIds = new HashSet<>();
    if (repOrderIds != null && !repOrderIds.isEmpty()) {
      repOrderIds.forEach(repOrderId -> {
        testDataClient.updateRepOrderSentenceOrderDate(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().plusMonths(-3)).build());
        String manualAcceleration = (fdcAccelerationType == FdcAccelerationType.POSITIVE)?"Y":null;
        int fdcId = testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", manualAcceleration, WAITING_ITEMS)).getId();
        fdcIds.add(fdcId);
        if (fdcAccelerationType.equals(FdcAccelerationType.PREVIOUS_FDC)) {
          testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", null, SENT));
        }
        FdcItemBuilder fdcItemBuilder = FdcItem.builder().fdcId(fdcId).userCreated("DCES").dateCreated(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
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
    return fdcIds;
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
