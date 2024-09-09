package uk.gov.justice.laa.crime.dces.integration.model;

import java.util.Map;

public record ContributionAckFromDrc(Data data, Map<String, String> meta) {
    public record Data(int id, Integer maatId, String errorText) {
    }

    public static ContributionAckFromDrc of(int concorContributionId, String errorText) {
        return new ContributionAckFromDrc(new Data(concorContributionId, null, errorText), Map.of());
    }
}
