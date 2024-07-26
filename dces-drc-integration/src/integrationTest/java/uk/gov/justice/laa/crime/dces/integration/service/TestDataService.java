package uk.gov.justice.laa.crime.dces.integration.service;

import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.SENT;
import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.WAITING_ITEMS;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.*;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem.FdcItemBuilder;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItemType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataService {

  private final MaatApiClient maatApiClient;

  /**
   * Create test data required for testing the FDC Delayed Pickup logic
   * (	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-365</a> for test specification.)
   * @param testType: One of the test types defined in enum FdcTestType
   * @param recordsToUpdate Number of records to update
   * @return A set of FDC IDs updated as required
   */
  public Set<Integer> createDelayedPickupTestData(FdcTestType testType, int recordsToUpdate){
    Set<Integer> repOrderIds = getFdcDelayCandidateRepOrderIds(5, LocalDate.of(2015,1,1), recordsToUpdate);
    Set<Integer> fdcIds = new HashSet<>();
    if (repOrderIds != null && !repOrderIds.isEmpty()) {
      repOrderIds.forEach(repOrderId -> {
        FdcContribution fdcContribution = createFdcContribution(repOrderId, "Y", "Y", null, WAITING_ITEMS);
        int fdcId = fdcContribution.getId();
        fdcIds.add(fdcId);
        FdcItem fdcItem = FdcItem.builder()
                .fdcId(fdcId)
                .userCreated("DCES")
                .dateCreated(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS))
                .build();
        createFdcItem(fdcItem);
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
    Set<Integer> repOrderIds = getFdcFastTrackCandidateRepOrderIds(-3, LocalDate.of(2015,1,1), recordsToUpdate);
    Set<Integer> fdcIds = new HashSet<>();
    if (repOrderIds != null && !repOrderIds.isEmpty()) {
      repOrderIds.forEach(repOrderId -> {
        maatApiClient.updateRepOrder(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().plusMonths(-3)).build());
        String manualAcceleration = (fdcAccelerationType == FdcAccelerationType.POSITIVE)?"Y":null;
        int fdcId = createFdcContribution(repOrderId, "Y", "Y", manualAcceleration, WAITING_ITEMS).getId();
        fdcIds.add(fdcId);
        if (fdcAccelerationType.equals(FdcAccelerationType.PREVIOUS_FDC)) {
          createFdcContribution(repOrderId, "Y", "Y", null, SENT);
        }
        FdcItemBuilder fdcItemBuilder = FdcItem.builder().fdcId(fdcId).userCreated("DCES").dateCreated(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
        if (fdcAccelerationType.equals(FdcAccelerationType.NEGATIVE)) {
          fdcItemBuilder = fdcItemBuilder.itemType(FdcItemType.LGFS).paidAsClaimed("Y").latestCostInd("Current");
          createFdcItem(fdcItemBuilder.build());
          fdcItemBuilder = fdcItemBuilder.itemType(FdcItemType.AGFS).adjustmentReason("Pre AGFS Transfer").paidAsClaimed("N").latestCostInd("Current");
        }
        createFdcItem(fdcItemBuilder.build());
        processNegativeTests(testType, repOrderId, fdcId, -7);
      });
    } else {
      throw new RuntimeException("No candidate rep orders found for delayed pickup test type " + testType);
    }
    return fdcIds;
  }

  private void processNegativeTests(FdcTestType testType, Integer repOrderId, Integer fdcId, int monthsAfterSysDate) {
    switch (testType) {
      case NEGATIVE_SOD -> maatApiClient.updateRepOrder(UpdateRepOrder.builder().repId(repOrderId)
          .sentenceOrderDate(LocalDate.now().plusMonths(monthsAfterSysDate)).build());
      case NEGATIVE_CCO -> maatApiClient.deleteCrownCourtOutcomes(repOrderId);
      case NEGATIVE_FDC_ITEM -> maatApiClient.deleteFdcItem(fdcId);
      case NEGATIVE_PREVIOUS_FDC -> maatApiClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, SENT.name(), WAITING_ITEMS));
      case NEGATIVE_FDC_STATUS -> maatApiClient.updateFdcContribution(new UpdateFdcContributionRequest(fdcId, repOrderId, null, SENT));
    }
  }

  private FdcContribution createFdcContribution(int repOrderId, String lgfsComplete, String agfsComplete, String manualAcceleration, FdcContributionsStatus status){
    ValidatableResponse response = maatApiClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, lgfsComplete, agfsComplete, manualAcceleration, status));
    return response.extract().body().as(FdcContribution.class);
  }

  private Set<Integer> getFdcDelayCandidateRepOrderIds(int delay, LocalDate dateReceived, int recordsToUpdate){
    ValidatableResponse repOrderResponse = maatApiClient.getFdcDelayedRepOrderIdList(delay, dateReceived, recordsToUpdate);
    return repOrderResponse.extract().body().as(Set.class);
  }


  private Set<Integer> getFdcFastTrackCandidateRepOrderIds(int delay, LocalDate dateReceived, int recordsToUpdate){
    ValidatableResponse repOrderResponse = maatApiClient.getFdcFastTrackRepOrderIdList(delay, dateReceived, recordsToUpdate);
    return repOrderResponse.extract().body().as(Set.class);
  }

  private FdcItem createFdcItem(FdcItem fdcItem){
    ValidatableResponse response = maatApiClient.createFdcItem(fdcItem);
    // map the LocalDate returned back into a LocalDateTime, as rest-assured exceptions with the returned format.
    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    FdcItem responseFdcItem = null;
    try {
      responseFdcItem = objectMapper.readValue(response.extract().body().asString(), FdcItem.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return responseFdcItem;
  }

  public FdcContribution getFdcContribution(int fdcId){
    ValidatableResponse response = maatApiClient.getFdcContribution(fdcId);
    return response.extract().body().as(FdcContribution.class);
  }
  public ContributionFileResponse getContributionFile(int fdcId){
    ValidatableResponse response = maatApiClient.getContributionFile(fdcId);
    return response.extract().body().as(ContributionFileResponse.class);
  }
  public List<Integer> updateConcorContributionStatus(final ConcorContributionStatus status, final int recordCount){
    UpdateConcorContributionStatusRequest request = UpdateConcorContributionStatusRequest.builder()
            .status(status)
            .recordCount(recordCount)
            .build();
    ValidatableResponse response = maatApiClient.updateConcorContributionStatus(request);
    return response.extract().body().as(List.class);
  }

  public ConcorContributionResponseDTO getConcorContribution(int concorId){
    ValidatableResponse response = maatApiClient.getConcorContribution(concorId);
    return response.extract().body().as(ConcorContributionResponseDTO.class);
  }

  public ContributionFileErrorResponse getContributionFileError(final int contributionFileId, final int contributionId){
    ValidatableResponse response = maatApiClient.getContributionFileError(contributionFileId, contributionId);
    ContributionFileErrorResponse errorResponse = null;
    try {
        return response.extract().body().as(ContributionFileErrorResponse.class);
    }
    catch (RuntimeException e){
      return null;
    }
  }

}
