package uk.gov.justice.laa.crime.dces.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateRepOrder;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItemType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.SENT;
import static uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus.WAITING_ITEMS;

@Slf4j
@Service
@RequiredArgsConstructor
public class FdcTestDataCreatorService {
    private static final String USER_AUDIT = "DCES";
    private static final String DATE_RECEIVED = "2015-01-01";

    private final TestDataClient testDataClient;

    private enum PickupType {
        DELAYED_PICKUP, FAST_TRACK
    }

    public static class FdcTestDataCreationException extends RuntimeException {
        public FdcTestDataCreationException(String message) {
            super(message);
        }
    }

    /**
     * Create test data required for testing the FDC Delayed Pickup logic.
     *
     * @param testType        One of the test types defined in enum FdcTestType
     * @param recordsToUpdate Number of records to update
     * @return A set of FDC IDs updated as required
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-365">DCES-365</a> for test data specification.
     */
    public Set<Integer> createDelayedPickupTestData(final FdcTestType testType, final int recordsToUpdate) {
        final var repOrderIds = testDataClient.getRepOrders(5, DATE_RECEIVED, recordsToUpdate, true, false);
        final var fdcIds = new HashSet<Integer>();
        if (repOrderIds != null && !repOrderIds.isEmpty()) {
            repOrderIds.forEach(repOrderId -> {
                final var fdcContribution = testDataClient.createFdcContribution(
                        new CreateFdcContributionRequest(repOrderId, "Y", "Y", null, WAITING_ITEMS));
                final int fdcId = fdcContribution.getId();
                fdcIds.add(fdcId);
                testDataClient.createFdcItems(FdcItem.builder().fdcId(fdcId).userCreated(USER_AUDIT)
                        .dateCreated(LocalDate.now())
                        .build());
                createAdditionalNegativeTypeTestData(testType, repOrderId, fdcId, PickupType.DELAYED_PICKUP);
            });
        } else {
            throw new FdcTestDataCreationException("No candidate rep orders found for delayed pickup test type " + testType);
        }
        return fdcIds;
    }

    /**
     * Create test data required for testing the FDC Fast Track logic.
     *
     * @param fdcAccelerationType One of the acceleration types defined in enum FdcAccelerationType
     * @param testType            One of the test types defined in enum FdcTestType
     * @param recordsToUpdate     Number of records to update
     * @return A set of FDC IDs updated as required
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-367">DCES-367</a>,
     * <a href="https://dsdmoj.atlassian.net/browse/DCES-377">DCES-377</a> and
     * <a href="https://dsdmoj.atlassian.net/browse/DCES-378">DCES-378</a> for test data specifications.
     * (this one method caters to all of them).
     */
    public Set<Integer> createFastTrackTestData(final FdcAccelerationType fdcAccelerationType,
                                                final FdcTestType testType, final int recordsToUpdate) {
        final var repOrderIds = testDataClient.getRepOrders(-3, DATE_RECEIVED, recordsToUpdate, false, true);
        final var fdcIds = new HashSet<Integer>();
        if (repOrderIds != null && !repOrderIds.isEmpty()) {
            repOrderIds.forEach(repOrderId -> {
                testDataClient.updateRepOrderSentenceOrderDate(UpdateRepOrder.builder().repId(repOrderId).sentenceOrderDate(LocalDate.now().minusMonths(3)).build());
                final String manualAcceleration = (fdcAccelerationType == FdcAccelerationType.POSITIVE) ? "Y" : null;
                final int fdcId = testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", manualAcceleration, WAITING_ITEMS)).getId();
                fdcIds.add(fdcId);
                if (fdcAccelerationType.equals(FdcAccelerationType.PREVIOUS_FDC)) {
                    testDataClient.createFdcContribution(new CreateFdcContributionRequest(repOrderId, "Y", "Y", null, SENT));
                }
                var fdcItemBuilder = FdcItem.builder().fdcId(fdcId).userCreated(USER_AUDIT).dateCreated(LocalDate.now());
                if (fdcAccelerationType.equals(FdcAccelerationType.NEGATIVE)) {
                    fdcItemBuilder = fdcItemBuilder.itemType(FdcItemType.LGFS).paidAsClaimed("Y").latestCostInd("Current");
                    testDataClient.createFdcItems(fdcItemBuilder.build());
                    fdcItemBuilder = fdcItemBuilder.itemType(FdcItemType.AGFS).adjustmentReason("Pre AGFS Transfer").paidAsClaimed("N").latestCostInd("Current");
                }
                if (fdcAccelerationType.equals(FdcAccelerationType.PREVIOUS_FDC) && testType.equals(FdcTestType.NEGATIVE_PREVIOUS_FDC)) {
                    fdcItemBuilder = fdcItemBuilder.adjustmentReason("Other");
                }
                testDataClient.createFdcItems(fdcItemBuilder.build());
                createAdditionalNegativeTypeTestData(testType, repOrderId, fdcId, PickupType.FAST_TRACK);
            });
        } else {
            throw new RuntimeException("No candidate rep orders found for fast track test type " + testType);
        }
        return fdcIds;
    }

    private void createAdditionalNegativeTypeTestData(final FdcTestType testType, final Integer repOrderId,
                                                      final Integer fdcId, final PickupType pickupType) {
        switch (testType) {
            case NEGATIVE_SOD -> {
                Map<String, Object> repOrderWithNullSOD = new HashMap<>();
                repOrderWithNullSOD.put("sentenceOrderDate", null);
                testDataClient.updateRepOrderSentenceOrderDateToNull(repOrderId, repOrderWithNullSOD);
            }
            case NEGATIVE_CCO -> testDataClient.deleteCrownCourtOutcomes(repOrderId);
            case NEGATIVE_FDC_ITEM -> testDataClient.deleteFdcItems(fdcId);
            case NEGATIVE_PREVIOUS_FDC -> testDataClient.updateFdcContribution(
                    new UpdateFdcContributionRequest(fdcId, repOrderId, SENT.name(), WAITING_ITEMS));
            case NEGATIVE_FDC_STATUS -> testDataClient.updateFdcContribution(
                    new UpdateFdcContributionRequest(fdcId, repOrderId, null, SENT));
        }
    }
}
