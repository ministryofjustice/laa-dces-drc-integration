package uk.gov.justice.laa.crime.dces.integration.client;

import io.restassured.response.ValidatableResponse;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.utils.RequestSpecificationBuilder;

import static net.serenitybdd.rest.SerenityRest.given;

@Component
public class MaatApiClient {

    private final String BASE_URL = "debt-collection-enforcement/";

    public ValidatableResponse getFdcList(String status) {
        final String CONTRIBUTION_FILES_URI = "fdc-contribution-files";
        return given()
                .spec(RequestSpecificationBuilder.getMaatAPICrimeApplyReqSpec())
                .param("status", status)
                .get(BASE_URL + CONTRIBUTION_FILES_URI)
                .then()
                .log()
                .all();
    }
}