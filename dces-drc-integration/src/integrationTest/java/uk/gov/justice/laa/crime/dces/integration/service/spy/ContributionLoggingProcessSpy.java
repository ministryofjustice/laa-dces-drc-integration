package uk.gov.justice.laa.crime.dces.integration.service.spy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionProcessedRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

/**
 * See {@link SpyFactory} for usage details.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class ContributionLoggingProcessSpy {
    @Singular
    private final List<Integer> concorContributionIds;         // Sent to the maat-api by ContributionClient.sendLogContributionProcessed(...)
    @Singular
    private final List<String> errorTexts;                     //    "    "    "
    @Singular
    private final List<Integer> contributionFileIds;           // Returned from maat-api by ContributionClient.sendLogContributionProcessed(...)

    private static ContributionLoggingProcessSpyBuilder builder() {
        throw new UnsupportedOperationException("Call SpyFactory.newContributionLoggingProcessSpyBuilder instead");
    }

    public static class ContributionLoggingProcessSpyBuilder {
        private final ContributionClient contributionClientSpy;

        private ContributionLoggingProcessSpyBuilder() {
            throw new UnsupportedOperationException("Call SpyFactory.newContributionLoggingProcessSpyBuilder instead");
        }

        ContributionLoggingProcessSpyBuilder(final ContributionClient contributionClientSpy) {
            this.contributionClientSpy = contributionClientSpy;
        }

        public ContributionLoggingProcessSpyBuilder traceSendLogContributionProcessed() {
            doAnswer(invocation -> {
                final var argument = (ContributionProcessedRequest) invocation.getArgument(0);
                concorContributionId(argument.getConcorId());
                errorText(argument.getErrorText());
                // Because ContributionClient is a proxied interface, cannot just call `invocation.callRealMethod()` here.
                // https://github.com/spring-projects/spring-boot/issues/36653
                final var result = (Integer) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
                contributionFileId(result);
                return result;
            }).when(contributionClientSpy).sendLogContributionProcessed(any());
            return this;
        }
    }
}
