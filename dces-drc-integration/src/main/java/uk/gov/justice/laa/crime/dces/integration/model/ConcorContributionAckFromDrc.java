package uk.gov.justice.laa.crime.dces.integration.model;

import java.util.Map;

public record ConcorContributionAckFromDrc(ConcorContributionAckData data, Map<String, String> meta) {
    public record ConcorContributionAckData(int concorContributionId, Integer maatId, String errorText) {
    }

    public static ConcorContributionAckFromDrc of(int concorContributionId, String errorText) {
        return new ConcorContributionAckFromDrc(new ConcorContributionAckData(concorContributionId, null, errorText), Map.of());
    }
}
