package uk.gov.justice.laa.crime.dces.integration.model;

import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;

import java.util.Map;

public record ConcorContributionReqForDrc(ConcorContributionReqData data, Map<String, String> meta) {
    /**
     * @param concorContributionObj This field cannot be named `concorContribution` because of Entity Framework used by Advantis.
     */
    public record ConcorContributionReqData(Long concorContributionId, CONTRIBUTIONS concorContributionObj) {
    }

    public static ConcorContributionReqForDrc of(final Long concorContributionId, final CONTRIBUTIONS concorContributionObj) {
        return new ConcorContributionReqForDrc(new ConcorContributionReqData(concorContributionId, concorContributionObj), Map.of());
    }

    public static ConcorContributionReqForDrc of(final Long concorContributionId, final CONTRIBUTIONS concorContributionObj, final Map<String, String> meta) {
        return new ConcorContributionReqForDrc(new ConcorContributionReqData(concorContributionId, concorContributionObj), meta);
    }
}
