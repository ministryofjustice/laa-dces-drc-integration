package uk.gov.justice.laa.crime.dces.integration.model;

import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import java.util.Map;
import java.util.Objects;

public record ConcorContributionReqForDrc(ConcorContributionReqData data, Map<String, String> meta) {
    /**
     * @param concorContributionObj This field cannot be named `concorContribution` because of Entity Framework used by Advantis.
     */
    public record ConcorContributionReqData(int concorContributionId, CONTRIBUTIONS concorContributionObj) {
        public ConcorContributionReqData(int concorContributionId, CONTRIBUTIONS concorContributionObj) {
            this.concorContributionId = concorContributionId;
            this.concorContributionObj = Objects.requireNonNull(concorContributionObj, "`concorContributionObj` must not be null");
        }
    }
    public ConcorContributionReqForDrc(ConcorContributionReqData data, Map<String, String> meta) {
        this.data = Objects.requireNonNull(data, "`data` must not be null");
        this.meta = Objects.requireNonNull(meta, "`meta` must not be null");
    }

    public static ConcorContributionReqForDrc of(final int concorContributionId, final CONTRIBUTIONS concorContributionObj) {
        return new ConcorContributionReqForDrc(new ConcorContributionReqData(concorContributionId, concorContributionObj), Map.of());
    }

    public static ConcorContributionReqForDrc of(final int concorContributionId, final CONTRIBUTIONS concorContributionObj, final Map<String, String> meta) {
        return new ConcorContributionReqForDrc(new ConcorContributionReqData(concorContributionId, concorContributionObj), meta);
    }
}
