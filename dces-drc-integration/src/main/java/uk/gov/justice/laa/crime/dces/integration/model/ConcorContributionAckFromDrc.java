package uk.gov.justice.laa.crime.dces.integration.model;

import org.springframework.http.ProblemDetail;

import java.util.Map;

public record ConcorContributionAckFromDrc(ConcorContributionAckData data, Map<String, String> meta) {
    public record ConcorContributionAckData(long concorContributionId, Long maatId, ProblemDetail report) {
        public String errorText() {
            return ProblemDetails.toErrorText(report);
        }
    }

    public static ConcorContributionAckFromDrc of(final long concorContributionId, final String errorText) {
        return new ConcorContributionAckFromDrc(new ConcorContributionAckData(concorContributionId, null,
                ProblemDetails.fromErrorText(errorText)), Map.of());
    }
}
