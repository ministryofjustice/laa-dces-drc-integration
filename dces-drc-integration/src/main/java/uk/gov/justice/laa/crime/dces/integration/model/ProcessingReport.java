package uk.gov.justice.laa.crime.dces.integration.model;

public record ProcessingReport(String title, String detail) {

    public static final String SUCCESS_TITLE = "Success";

    public boolean isSuccessReport() {
        return SUCCESS_TITLE.equals(title);
    }
}
