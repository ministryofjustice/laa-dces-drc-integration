package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Builder;

import java.util.Map;

@Builder
public record ConcorContributionAckFromDrc(ConcorContributionAckData data, Map<String, String> meta) {

    @Builder
    public record ConcorContributionAckData(long concorContributionId, Long maatId, ProcessingReport report) {
    }

}
