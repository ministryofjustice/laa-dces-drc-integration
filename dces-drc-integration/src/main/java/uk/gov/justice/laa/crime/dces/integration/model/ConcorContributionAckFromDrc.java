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
        @NotNull(message = "concorContributionId cannot be null.")
        @Positive(message = "concorContributionId must be positive.")
        Long concorContributionId,
        @NotNull(message = "maatId cannot be null.")
        @Positive(message = "maatId must be positive.")
        Long maatId,
        @Valid
        @NotNull(message = "reportId cannot be null.")
        ProcessingReport report) {
    }

}
