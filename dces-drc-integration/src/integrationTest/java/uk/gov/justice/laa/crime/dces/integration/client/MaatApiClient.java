package uk.gov.justice.laa.crime.dces.integration.client;

import io.restassured.response.ValidatableResponse;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.utils.RequestSpecificationBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static net.serenitybdd.rest.SerenityRest.given;

@Component
public class MaatApiClient {

    private static final String DCES_BASE_URL = "debt-collection-enforcement/";
    private static final String ASSESSMENT_BASE_URL = "assessment/";

    public ValidatableResponse getFdcList(String status) {
        final String CONTRIBUTION_FILES_URI = "fdc-contribution-files";
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .param("status", status)
                .get(DCES_BASE_URL + CONTRIBUTION_FILES_URI)
                .then()
                .log()
                .all();
    }

    public ValidatableResponse getFdcFastTrackRepOrderIdList(int delay, LocalDate dateRecieved, int numRecords) {
        final String REP_ORDERS_URI = "rep-orders";
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .param("fdcFastTrack", "true")
                .param("delay", delay)
                .param("dateReceived", dateRecieved.format(DateTimeFormatter.ISO_DATE))
                .param("numRecords", numRecords)
                .get(ASSESSMENT_BASE_URL + REP_ORDERS_URI)
                .then()
                .log()
                .all();
    }

    public ValidatableResponse getFdcDelayedRepOrderIdList(int delay, LocalDate dateRecieved, int numRecords) {
        final String REP_ORDERS_URI = "rep-orders";
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .param("fdcDelayedPickup", "true")
                .param("delay", delay)
                .param("dateReceived", dateRecieved.format(DateTimeFormatter.ISO_DATE))
                .param("numRecords", numRecords)
                .get(ASSESSMENT_BASE_URL + REP_ORDERS_URI)
                .then()
                .log()
                .all();
    }
}