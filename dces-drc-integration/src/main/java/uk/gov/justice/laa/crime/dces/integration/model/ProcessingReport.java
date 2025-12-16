package uk.gov.justice.laa.crime.dces.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ProcessingReport(String title, String detail) {

    public static final String SUCCESS_TITLE = "Success";

    @JsonIgnore
    public boolean isSuccessReport() {
        return SUCCESS_TITLE.equals(title);
    }
}
