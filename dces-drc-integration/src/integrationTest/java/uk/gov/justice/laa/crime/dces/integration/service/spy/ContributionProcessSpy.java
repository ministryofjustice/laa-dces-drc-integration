package uk.gov.justice.laa.crime.dces.integration.service.spy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.springframework.http.HttpStatus;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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

        public ContributionProcessSpyBuilder traceGetContributionsActive() {
            doAnswer(invocation -> {
                // Because ContributionClient is a proxied interface, cannot just call `invocation.callRealMethod()` here.
                // https://github.com/spring-projects/spring-boot/issues/36653
                @SuppressWarnings("unchecked")
                final var result = (List<ConcurContribEntry>) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
                activeIds(result.stream().map(ConcurContribEntry::getConcorContributionId).collect(Collectors.toSet()));
                return result;
            }).when(contributionClientSpy).getContributions("ACTIVE", 0, 10);
            return this;
        }

        public ContributionProcessSpyBuilder traceAndFilterGetContributionsActive(final List<Integer> activeIds) {
            final var idSet = Set.copyOf(activeIds); // defensive copy
            doAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                var result = (List<ConcurContribEntry>) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
                result = result.stream().filter(concurContribEntry -> idSet.contains(concurContribEntry.getConcorContributionId())).toList();
                activeIds(result.stream().map(ConcurContribEntry::getConcorContributionId).collect(Collectors.toSet()));
                return result;
            }).when(contributionClientSpy).getContributions("ACTIVE", 0, 10);
            return this;
        }

        public ContributionProcessSpyBuilder traceAndStubSendContributionUpdate(final Predicate<Integer> stubResults) {
            doAnswer(invocation -> {
                final int concorContributionId = ((ConcorContributionReqForDrc) invocation.getArgument(0)).data().concorContributionId();
                sentId(concorContributionId);
                if (!stubResults.test(concorContributionId)) {
                    throw new MaatApiClientException(HttpStatus.BAD_REQUEST, "BAD_REQUEST");
                }
                return null;
            }).when(drcClientSpy).sendConcorContributionReqToDrc(any());
            return this;
        }

        public ContributionProcessSpyBuilder traceUpdateContributions() {
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
            return this;
        }
    }
}
