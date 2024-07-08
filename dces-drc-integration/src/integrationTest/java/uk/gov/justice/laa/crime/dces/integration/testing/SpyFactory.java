package uk.gov.justice.laa.crime.dces.integration.testing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;

import java.util.List;

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

    @Autowired
    private TestDataClient testDataClient;

    public ContributionProcessSpy.ContributionProcessSpyBuilder newContributionProcessSpyBuilder() {
        return new ContributionProcessSpy.ContributionProcessSpyBuilder(contributionClientSpy, drcClientSpy);
    }

    public DrcLoggingProcessSpy.DrcLoggingProcessSpyBuilder newDrcLoggingProcessSpyBuilder() {
        return new DrcLoggingProcessSpy.DrcLoggingProcessSpyBuilder(contributionClientSpy);
    }

    public List<Integer> updateConcorContributionStatus(final ConcorContributionStatus status, final int recordCount) {
        final var request = UpdateConcorContributionStatusRequest.builder()
                .status(status)
                .recordCount(recordCount)
                .build();
        return testDataClient.updateConcorContributionStatus(request);
    }
}
