package uk.gov.justice.laa.crime.dces.integration.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import lombok.Builder;

@Builder
public record ConcorContributionAckFromDrc(
    @Valid
    @NotNull(message = "data cannot be null.")
    ConcorContributionAckData data,
    Map<String, String> meta) {

    @Builder
    public record ConcorContributionAckData(
        @NotNull(message = "Concor Contribution ID cannot be null.")
        @Positive(message = "Concor Contribution ID must be positive.")
        Long concorContributionId,

        /*
          No check for positive numbers on maatId because if the MAAT ID is invalid, e.g. < 1
          the invalid value will be returned in this field.
         */
        @NotNull(message = "MAAT ID cannot be null.")
        Long maatId,

        @Valid
        @NotNull(message = "Report cannot be null.")
        ProcessingReport report) {
    }

}
