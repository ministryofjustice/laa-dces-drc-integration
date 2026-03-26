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
        @NotNull(message = "FDC ID cannot be null.")
        @Positive(message = "FDC ID must be positive.")
        Long fdcId,

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