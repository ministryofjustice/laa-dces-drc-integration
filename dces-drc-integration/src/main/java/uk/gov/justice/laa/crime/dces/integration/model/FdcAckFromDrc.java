package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Builder;
import org.springframework.http.ProblemDetail;

import java.util.Map;

@Builder
public record FdcAckFromDrc(FdcAckData data, Map<String, String> meta) {

    @Builder
    public record FdcAckData(Long fdcId, Long maatId, ProblemDetail report) {
        public String errorText() {
            return report.getTitle();
        }
    }

}