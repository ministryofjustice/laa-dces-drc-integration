package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Builder;
import org.springframework.http.ProblemDetail;

import java.util.Map;

@Builder
public record ConcorContributionAckFromDrc(ConcorContributionAckData data, Map<String, String> meta) {

    @Builder
    public record ConcorContributionAckData(long concorContributionId, Long maatId, ProblemDetail report) {
        public String errorText() {
            return report.getTitle();
        }
    }

}
