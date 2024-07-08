package uk.gov.justice.laa.crime.dces.integration.testing;

import lombok.*;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.SendContributionFileDataToDrcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionResponseDTO;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

/**
 * See {@link SpyFactory} for usage details.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class DrcLoggingProcessSpy {
    @Singular
    private final List<Integer> concorContributionIds;         // Sent to the maat-api by ContributionClient.sendLogContributionProcessed(...)
    @Singular
    private final List<String> errorTexts;                     //    "    "    "
    @Singular
    private final List<Integer> contributionFileIds;           // Returned from maat-api by ContributionClient.sendLogContributionProcessed(...)

    private static DrcLoggingProcessSpyBuilder builder() {
        throw new UnsupportedOperationException("Call SpyFactory.newDrcLoggingProcessSpyBuilder instead");
    }

    public static class DrcLoggingProcessSpyBuilder {
        private final ContributionClient contributionClientSpy;

        private DrcLoggingProcessSpyBuilder() {
            throw new UnsupportedOperationException("Call SpyFactory.newDrcLoggingProcessSpyBuilder instead");
        }

        DrcLoggingProcessSpyBuilder(final ContributionClient contributionClientSpy) {
            this.contributionClientSpy = contributionClientSpy;
        }

        public void instrumentSendLogContributionProcessed() {
            doAnswer(invocation -> {
                final var argument = (UpdateLogContributionRequest) invocation.getArgument(0);
                concorContributionId(argument.getConcorId());
                errorText(argument.getErrorText());
                // Because ContributionClient is a proxied interface, cannot just call `invocation.callRealMethod()` here.
                // https://github.com/spring-projects/spring-boot/issues/36653
                final var result = (Integer) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
                contributionFileId(result);
                return result;
            }).when(contributionClientSpy).sendLogContributionProcessed(any());
        }
    }
}
