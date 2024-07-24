package uk.gov.justice.laa.crime.dces.integration.client;

import io.restassured.response.ValidatableResponse;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.utils.RequestSpecificationBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static net.serenitybdd.rest.SerenityRest.given;

@Component
public class MaatApiClient {

    private static final String DCES_BASE_URL = "debt-collection-enforcement";
    private static final String CONTRIBUTION_FILES_URI = "/fdc-contribution-files";
    private static final String FDC_ITEMS_URI = "/fdc-items";
    private static final String FDC_CONTRIBUTION_URI = "/fdc-contribution";
    private static final String REP_ORDERS_BASE_URL = "assessment/rep-orders";
    private static final String CCOUTCOME_URI = "/cc-outcome";


    public ValidatableResponse getFdcList(String status) {

        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .param("status", status)
                .get(DCES_BASE_URL + CONTRIBUTION_FILES_URI)
                .then()
                .log()
                .all();
    }

    public ValidatableResponse getFdcFastTrackRepOrderIdList(int delay, LocalDate dateRecieved, int numRecords) {
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .param("fdcFastTrack", "true")
                .param("delay", delay)
                .param("dateReceived", dateRecieved.format(DateTimeFormatter.ISO_DATE))
                .param("numRecords", numRecords)
                .get(REP_ORDERS_BASE_URL)
                .then()
                .log()
                .all();
    }

    public ValidatableResponse getFdcDelayedRepOrderIdList(int delay, LocalDate dateRecieved, int numRecords) {
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .param("fdcDelayedPickup", "true")
                .param("delay", delay)
                .param("dateReceived", dateRecieved.format(DateTimeFormatter.ISO_DATE))
                .param("numRecords", numRecords)
                .get(REP_ORDERS_BASE_URL)
                .then()
                .log()
                .all();
    }

    //@PostExchange("/debt-collection-enforcement/fdc-contribution")
    //    @Valid
    //    FdcContribution createFdcContribution(@RequestBody CreateFdcContributionRequest fdcContribution);
    public ValidatableResponse createFdcContribution(CreateFdcContributionRequest requestBody) {
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .body(requestBody)
                .post(DCES_BASE_URL + FDC_CONTRIBUTION_URI)
                .then()
                .log()
                .all();
    }

    //    @PostExchange("/debt-collection-enforcement/fdc-items")
    //    @Valid
    //    void createFdcItems(@Valid @RequestBody final FdcItem fdcItemDTO);
    public ValidatableResponse createFdcItem(FdcItem requestBody) {
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .body(requestBody)
                .post(DCES_BASE_URL + FDC_ITEMS_URI)
                .then()
                .log()
                .all();
    }

    //    @DeleteExchange("/debt-collection-enforcement/fdc-items/fdc-id/{fdcId}")
    //    @Valid
    //    void deleteFdcItems(@NotNull @PathVariable final Integer fdcId);
    public ValidatableResponse deleteFdcItem(int fdcId) {
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .pathParam("fdc-id", fdcId)
                .delete(DCES_BASE_URL + FDC_ITEMS_URI)
                .then()
                .log()
                .all();
    }

//    @DeleteExchange("/assessment/rep-orders/cc-outcome/rep-order/{repId}")
//    @Valid
//    void deleteCrownCourtOutcomes(@PathVariable Integer repId);
    public ValidatableResponse deleteCrownCourtOutcomes(int repId) {
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .pathParam("rep-order", repId)
                .delete(REP_ORDERS_BASE_URL + CCOUTCOME_URI)
                .then()
                .log()
                .all();
    }
}