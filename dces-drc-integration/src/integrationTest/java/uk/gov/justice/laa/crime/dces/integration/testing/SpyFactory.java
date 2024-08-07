package uk.gov.justice.laa.crime.dces.integration.testing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionFileErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;
import uk.gov.justice.laa.crime.dces.integration.service.FdcTestDataCreatorService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Entry-point for working with the ContributionProcessSpy and ContributionProcessSpyBuilder classes (and other spies
 * as they are developed).
 * <p>
 * To use the spy, auto-wire an instance of this factory into your `@SpringBootTest`-annotated test class, call the
 * method that creates a builder, then call methods on the builder before, or while, the class or method under test
 * executes. Then call the builder's `build()` method to create a data class which can be used to validate assertions.
 */
@Component
public class SpyFactory {
    @SpyBean
    private ContributionClient contributionClientSpy;

    @SpyBean
    private DrcClient drcClientSpy;

    @SpyBean
    private FdcClient fdcClientSpy;

    @SpyBean
    private FdcTestDataCreatorService fdcTestDataCreatorService;

    @Autowired
    private TestDataClient testDataClient;

    public ContributionProcessSpy.ContributionProcessSpyBuilder newContributionProcessSpyBuilder() {
        return new ContributionProcessSpy.ContributionProcessSpyBuilder(contributionClientSpy, drcClientSpy);
    }

    public ContributionLoggingProcessSpy.ContributionLoggingProcessSpyBuilder newContributionLoggingProcessSpyBuilder() {
        return new ContributionLoggingProcessSpy.ContributionLoggingProcessSpyBuilder(contributionClientSpy);
    }

    public FdcProcessSpy.FdcProcessSpyBuilder newFdcProcessSpyBuilder() {
        return new FdcProcessSpy.FdcProcessSpyBuilder(fdcClientSpy, drcClientSpy);
    }

    public FdcLoggingProcessSpy.FdcLoggingProcessSpyBuilder newFdcLoggingProcessSpyBuilder() {
        return new FdcLoggingProcessSpy.FdcLoggingProcessSpyBuilder(fdcClientSpy);
    }

    public List<Integer> updateConcorContributionStatus(final ConcorContributionStatus status, final int recordCount) {
        final var request = UpdateConcorContributionStatusRequest.builder()
                .status(status)
                .recordCount(recordCount)
                .build();
        return testDataClient.updateConcorContributionStatus(request);
    }

    public Set<Integer> createFdcDelayedPickupTestData(final FdcTestType testType, final int recordsToUpdate) {
        return fdcTestDataCreatorService.createDelayedPickupTestData(testType, recordsToUpdate);
    }

    public Set<Integer> createFastTrackTestData(
            final FdcAccelerationType fdcAccelerationType, final FdcTestType testType, final int recordsToUpdate) {
        return fdcTestDataCreatorService.createFastTrackTestData(fdcAccelerationType, testType, recordsToUpdate);
    }

    /**
     * Get a contribution_file_error entity, but handle 404 by returning Optional.empty() instead of an exception.
     * <p>
     * Testing utility method.
     */
    public Optional<ContributionFileErrorResponse> getContributionFileErrorOptional(final int contributionFileId, final int contributionId) {
        try {
            return Optional.of(testDataClient.getContributionFileError(contributionFileId, contributionId));
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }
}
