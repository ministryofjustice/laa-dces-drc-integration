package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;

import java.util.Map;

@Data
public class ContributionDataForDrc {
    private final CONTRIBUTIONS data;
    private final Map<String, String> meta;

    private ContributionDataForDrc(CONTRIBUTIONS data, Map<String, String> meta) {
        this.data = data;
        this.meta = meta;
    }

    public static ContributionDataForDrc of(String contributionIdStr, CONTRIBUTIONS contribution) {
        return new ContributionDataForDrc(contribution, contributionIdStr != null ? Map.of("contributionId", contributionIdStr) : Map.of());
    }
}
