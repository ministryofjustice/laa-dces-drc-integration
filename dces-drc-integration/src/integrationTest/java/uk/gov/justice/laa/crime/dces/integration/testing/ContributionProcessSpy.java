package uk.gov.justice.laa.crime.dces.integration.testing;

import lombok.*;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.SendContributionFileDataToDrcRequest;

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
public class ContributionProcessSpy {
    private final Set<Integer> activeIds;         // Returned from maat-api by ContributionClient.getContributions("ACTIVE")
    @Singular
    private final Set<Integer> sentIds;           // Sent to the DRC by DrcClient.sendContributionUpdate(...)
    private final Set<Integer> xmlCcIds;          // Sent to maat-api by ContributionClient.updateContribution(...)
    private final int recordsSent;                //  "    "    "
    private final String xmlContent;              //  "    "    "
    private final String xmlFileName;             //  "    "    "
    private final Integer xmlFileResult;          // Returned from maat-api by ContributionClient.updateContribution(...)

    private static ContributionProcessSpyBuilder builder() {
        throw new UnsupportedOperationException("Call SpyFactory.newContributionProcessSpyBuilder instead");
    }

    public static class ContributionProcessSpyBuilder {
        private final ContributionClient contributionClientSpy;
        private final DrcClient drcClientSpy;

        private ContributionProcessSpyBuilder() {
            throw new UnsupportedOperationException("Call SpyFactory.newContributionProcessSpyBuilder instead");
        }

        ContributionProcessSpyBuilder(final ContributionClient contributionClientSpy, final DrcClient drcClientSpy) {
            this.contributionClientSpy = contributionClientSpy;
            this.drcClientSpy = drcClientSpy;
        }

        public void instrumentGetContributionsActive() {
            doAnswer(invocation -> {
                // Because ContributionClient is a proxied interface, cannot just call `invocation.callRealMethod()` here.
                // https://github.com/spring-projects/spring-boot/issues/36653
                @SuppressWarnings("unchecked")
                final var result = (List<ConcurContribEntry>) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
                activeIds(result.stream().map(ConcurContribEntry::getConcorContributionId).collect(Collectors.toSet()));
                return result;
            }).when(contributionClientSpy).getContributions("ACTIVE");
        }

        public void instrumentStubbedSendContributionUpdate(Boolean stubbedResult) {
            doAnswer(invocation -> {
                sentId(((SendContributionFileDataToDrcRequest) invocation.getArgument(0)).getContributionId());
                return stubbedResult;
            }).when(drcClientSpy).sendContributionUpdate(any());
        }

        public void instrumentUpdateContributions() {
            doAnswer(invocation -> {
                final var data = (ContributionUpdateRequest) invocation.getArgument(0);
                xmlCcIds(data.getConcorContributionIds().stream().map(Integer::valueOf).collect(Collectors.toSet()));
                recordsSent(data.getRecordsSent());
                xmlContent(data.getXmlContent());
                xmlFileName(data.getXmlFileName());
                final var result = (Integer) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
                xmlFileResult(result);
                return result;
            }).when(contributionClientSpy).updateContributions(any());
        }
    }
}
