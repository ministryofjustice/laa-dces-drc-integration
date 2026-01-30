package uk.gov.justice.laa.crime.dces.integration.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import lombok.Builder;

@Builder
public record FdcAckFromDrc(
    @Valid
    @NotNull(message = "data cannot be null.")
    FdcAckData data,
    Map<String, String> meta) {

    @Builder
    public record FdcAckData(
        @NotNull(message = "fdcId cannot be null.")
        @Positive(message = "fdcId must be positive.")
        Long fdcId,
        @NotNull(message = "maatId cannot be null.")
        @Positive(message = "maatId must be positive.")
        Long maatId,
        @Valid
        @NotNull(message = "report cannot be null.")
        ProcessingReport report) {
    }

}