package uk.gov.justice.laa.crime.dces.integration.model;

import org.springframework.http.ProblemDetail;

import java.util.Map;

public record FdcAckFromDrc(FdcAckData data, Map<String, String> meta) {
    public record FdcAckData(int fdcId, Integer maatId, ProblemDetail report) {
        public String errorText() {
            return ProblemDetails.toErrorText(report);
        }
    }

    public static FdcAckFromDrc of(final int fdcId, final String errorText) {
        return new FdcAckFromDrc(new FdcAckData(fdcId, null,
                ProblemDetails.fromErrorText(errorText)), Map.of());
    }
}
