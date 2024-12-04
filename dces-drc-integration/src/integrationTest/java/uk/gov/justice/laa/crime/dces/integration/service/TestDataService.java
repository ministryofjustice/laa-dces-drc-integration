package uk.gov.justice.laa.crime.dces.integration.service;

import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.SENT;
import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.WAITING_ITEMS;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.*;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem.FdcItemBuilder;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItemType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;


@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataService {

    private static final String USER_AUDIT = "DCES";
    private static final LocalDate DATE_RECEIVED = LocalDate.of(2015,1,1);

    private final MaatApiClient maatApiClient;

    public static class TestDataServiceException extends RuntimeException {
        public TestDataServiceException(String message) {
            super(message);
        }
    }

  /**
   * Create test data required for testing the FDC Delayed Pickup logic
   * (	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-365</a> for test specification.)
   * @param testType: One of the test types defined in enum FdcTestType
   * @param recordsToUpdate Number of records to update
   * @return A set of FDC IDs updated as required
   */
  public Set<Long> createDelayedPickupTestData(FdcTestType testType, int recordsToUpdate){
    Set<Long> repOrderIds = getFdcDelayCandidateRepOrderIds(5, DATE_RECEIVED, recordsToUpdate);
    Set<Long> fdcIds = new HashSet<>();
    if (repOrderIds != null && !repOrderIds.isEmpty()) {
      repOrderIds.forEach(repOrderId -> {
        FdcContribution fdcContribution = createFdcContribution(repOrderId, "Y", "Y", null, WAITING_ITEMS);
        long fdcId = fdcContribution.getId();
        fdcIds.add(fdcId);
        FdcItem fdcItem = FdcItem.builder()
                .fdcId(fdcId)
                .userCreated(USER_AUDIT)
                .dateCreated(LocalDate.now())
                .build();
        createFdcItem(fdcItem);
        createAdditionalNegativeTypeTestData(testType, repOrderId, fdcId);
      });
    } else {
      throw new TestDataServiceException("No candidate rep orders found for delayed pickup test type " + testType);
    }
    return fdcIds;
  }

  /**
   * Create test data required for testing the FDC Fast Track  logic
   * (	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-367</a>,
   * <a href="https://dsdmoj.atlassian.net/browse/DCES-377">DCES-377</a> and
   * <a href="https://dsdmoj.atlassian.net/browse/DCES-378">DCES-378</a>for test specification (this one method caters to all of them).)
   * @param fdcAccelerationType: One of the acceleration types defined in enum FdcAccelerationType
   * @param testType: One of the test types defined in enum FdcTestType
   * @param recordsToUpdate Number of records to update
   * @return A set of FDC IDs updated as required
   */

  public Set<Long> createFastTrackTestData( FdcAccelerationType fdcAccelerationType, FdcTestType testType, int recordsToUpdate){
    Set<Long> repOrderIds = getFdcFastTrackCandidateRepOrderIds(-3, DATE_RECEIVED, recordsToUpdate);
    Set<Long> fdcIds = new HashSet<>();
    if(repOrderIds == null || repOrderIds.isEmpty() ){
      throw new TestDataServiceException("No candidate rep orders found for fast-track pickup test type " + testType);
    }
    repOrderIds.forEach(repOrderId -> {
      maatApiClient.updateRepOrder(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().minusMonths(3)).build());
      String manualAcceleration = (fdcAccelerationType == FdcAccelerationType.POSITIVE)?"Y":null;
      long fdcId = createFdcContribution(repOrderId, "Y", "Y", manualAcceleration, WAITING_ITEMS).getId();
      fdcIds.add(fdcId);

      FdcItemBuilder fdcItemBuilder = FdcItem.builder().fdcId(fdcId).userCreated(USER_AUDIT).dateCreated(LocalDate.now());
      if (fdcAccelerationType.equals(FdcAccelerationType.NEGATIVE)) {
        fdcItemBuilder = fdcItemBuilder.itemType(FdcItemType.LGFS).paidAsClaimed("Y").latestCostInd("Current");
        createFdcItem(fdcItemBuilder.build());
        fdcItemBuilder = fdcItemBuilder.itemType(FdcItemType.AGFS).adjustmentReason("Pre AGFS Transfer").paidAsClaimed("N").latestCostInd("Current");
      }
      if (fdcAccelerationType.equals(FdcAccelerationType.PREVIOUS_FDC)) {
        createFdcContribution(repOrderId, "Y", "Y", null, SENT);
        if(testType.equals(FdcTestType.NEGATIVE_PREVIOUS_FDC)){
          fdcItemBuilder = fdcItemBuilder.adjustmentReason("Other");
        }
      }
      createFdcItem(fdcItemBuilder.build());
      createAdditionalNegativeTypeTestData(testType, repOrderId, fdcId);
    });
  return fdcIds;
  }

  private void createAdditionalNegativeTypeTestData(FdcTestType testType, Long repOrderId, Long fdcId) {
    switch (testType) {
      case NEGATIVE_SOD -> {
            Map<String, Object> repOrderWithNullSOD = new HashMap<>();
            repOrderWithNullSOD.put("sentenceOrderDate", null);
            maatApiClient.updateRepOrderSentenceOrderDateToNull(repOrderId, repOrderWithNullSOD);
        }
      case NEGATIVE_CCO -> maatApiClient.deleteCrownCourtOutcomes(repOrderId);
      case NEGATIVE_FDC_ITEM -> maatApiClient.deleteFdcItems(fdcId);
      case NEGATIVE_PREVIOUS_FDC -> maatApiClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, SENT.name(), WAITING_ITEMS));
      case NEGATIVE_FDC_STATUS -> maatApiClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, null, SENT));
    }
  }

  private FdcContribution createFdcContribution(long repOrderId, String lgfsComplete, String agfsComplete, String manualAcceleration, FdcContributionsStatus status){
    return maatApiClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, lgfsComplete, agfsComplete, manualAcceleration, status));
  }

  private Set<Long> getFdcDelayCandidateRepOrderIds(int delay, LocalDate dateReceived, int recordsToUpdate){
    return maatApiClient.getFdcDelayedRepOrderIdList(delay, dateReceived, recordsToUpdate);
  }


  private Set<Long> getFdcFastTrackCandidateRepOrderIds(int delay, LocalDate dateReceived, int recordsToUpdate){
    return maatApiClient.getFdcFastTrackRepOrderIdList(delay, dateReceived, recordsToUpdate);
  }

  private FdcItem createFdcItem(FdcItem fdcItem){
    return maatApiClient.createFdcItem(fdcItem);
  }

  public FdcContribution getFdcContribution(long fdcId){
    return maatApiClient.getFdcContribution(fdcId);
  }
  public ContributionFileResponse getContributionFile(long fdcId){
    return maatApiClient.getContributionFile(fdcId);
  }
  public List<Long> updateConcorContributionStatus(final ConcorContributionStatus status, final int recordCount){
    UpdateConcorContributionStatusRequest request = UpdateConcorContributionStatusRequest.builder()
            .status(status)
            .recordCount(recordCount)
            .build();
    return maatApiClient.updateConcorContributionStatus(request);
  }

  public ConcorContributionResponseDTO getConcorContribution(Long concorId){
    return maatApiClient.getConcorContribution(concorId);
  }

  public ContributionFileErrorResponse getContributionFileError(final long contributionFileId, final long contributionId){
    return maatApiClient.getContributionFileError(contributionFileId, contributionId);

  }

}
