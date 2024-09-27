package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;

import java.util.Map;

@Data
public class ConcorContributionReqForDrc {
    private final ConcorContributionReqData data;
    private final Map<String, String> meta;

    @Data
    public static class ConcorContributionReqData {
        private final int concorContributionId;
        /** This field cannot be `concorContribution` because of Entity Framework used by Advantis. */
        private final CONTRIBUTIONS concorContributionObj;
    }

    public static ConcorContributionReqForDrc of(int concorContributionId, CONTRIBUTIONS concorContributionObj) {
        return new ConcorContributionReqForDrc(new ConcorContributionReqData(concorContributionId, concorContributionObj), Map.of());
    }
}
