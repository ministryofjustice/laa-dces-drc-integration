package uk.gov.justice.laa.crime.dces.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProcessingReport(
    @NotNull(message = "title must not be null.")
    @Size(min = 1, max = 200, message = "title cannot be less than 1 character.")
    @Size(max = 200, message = "title cannot be more than 200 characters.")
    String title,
    @NotNull(message = "detail must not be null.")
    @Pattern(regexp = UTC_ONLY_ISO_8601, message = "detail must be ISO 8601 format explicitly in UTC, using either Z or +00:00.")
    String detail) {

    public static final String SUCCESS_TITLE = "Success";
    private static final String UTC_ONLY_ISO_8601 =
      "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|\\+00:00)$";
    @JsonIgnore
    public boolean isSuccessReport() {
        return SUCCESS_TITLE.equals(title);
    }
}
