package uk.gov.justice.laa.crime.dces.integration.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.ValidatableResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionResponseDTO;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionFileErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionFileResponse;
import uk.gov.justice.laa.crime.dces.integration.model.external.CreateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcItem;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateFdcContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateRepOrder;
import uk.gov.justice.laa.crime.dces.integration.utils.RequestSpecificationBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.serenitybdd.rest.SerenityRest.given;

@Component
public class MaatApiClient {

    private static final String DCES_BASE_URL = "debt-collection-enforcement";
    private static final String FDC_CONTRIBUTION_FILES_URI = "/fdc-contribution-files";
    private static final String CONTRIBUTION_FILE_URI = "/contribution-file";
    private static final String CONCOR_CONTRIBUTION_STATUS_URI = "/concor-contribution-status";
    private static final String CONCOR_CONTRIBUTION_URI = "/concor-contribution";
    private static final String FDC_ITEMS_URI = "/fdc-items";
    private static final String FDC_CONTRIBUTION_URI = "/fdc-contribution";
    private static final String REP_ORDERS_BASE_URL = "assessment/rep-orders";
    private static final String CCOUTCOME_URI = "/cc-outcome/rep-order/{repId}";

    @Autowired
    private RequestSpecificationBuilder builder;

    private static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).findAndRegisterModules();

    public List<FdcContribution> getFdcList(String status) {

        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .param("status", status)
                .get(DCES_BASE_URL + FDC_CONTRIBUTION_FILES_URI)
                .then()
                .log()
                .all();


        return response.extract().body().jsonPath().getList("fdcContributions", FdcContribution.class);
    }

    public ValidatableResponse updateRepOrderSentenceOrderDateToNull(long repOrderId, Map<String, Object> repOrderFields) {
        String repOrderIdParameter = "/{repId}";
        return given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .body(repOrderFields)
                .pathParam("repId", repOrderId)
                .patch(REP_ORDERS_BASE_URL + repOrderIdParameter)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);
    }


    public Set<Long> getFdcFastTrackRepOrderIdList(int delay, LocalDate dateRecieved, int numRecords) {
        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .param("fdcFastTrack", "true")
                .param("delay", delay)
                .param("dateReceived", dateRecieved.format(DateTimeFormatter.ISO_DATE))
                .param("numRecords", numRecords)
                .get(REP_ORDERS_BASE_URL)
                .then()
                .log()
                .all();

        return Set.copyOf(response.extract().body().jsonPath().getList(".", Long.class));
    }

    public Set<Long> getFdcDelayedRepOrderIdList(int delay, LocalDate dateRecieved, int numRecords) {
        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .param("fdcDelayedPickup", "true")
                .param("delay", delay)
                .param("dateReceived", dateRecieved.format(DateTimeFormatter.ISO_DATE))
                .param("numRecords", numRecords)
                .get(REP_ORDERS_BASE_URL)
                .then()
                .log()
                .all();
        return Set.copyOf(response.extract().body().jsonPath().getList(".", Long.class));
    }

    public FdcContribution createFdcContribution(CreateFdcContributionRequest requestBody) {
        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .body(requestBody)
                .post(DCES_BASE_URL + FDC_CONTRIBUTION_URI)
                .then()
                .log()
                .all();
        return response.extract().body().as(FdcContribution.class);
    }

    public FdcItem createFdcItem(FdcItem requestBody) {
        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .body(requestBody)
                .post(DCES_BASE_URL + FDC_ITEMS_URI)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);
        FdcItem responseFdcItem = null;
        try {
            responseFdcItem = objectMapper.readValue(response.extract().body().asString(), FdcItem.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return responseFdcItem;
    }

    public ValidatableResponse deleteFdcItems(long fdcId) {
        String params = "/fdc-id/{fdc-id}";
        return given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .pathParam("fdc-id", fdcId)
                .delete(DCES_BASE_URL + FDC_ITEMS_URI + params)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);
    }

    public ValidatableResponse deleteCrownCourtOutcomes(long repId) {
        return given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .pathParam("repId", repId)
                .delete(REP_ORDERS_BASE_URL + CCOUTCOME_URI)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);
    }

    public ValidatableResponse updateFdcContribution(UpdateFdcContributionRequest fdcContribution) {
        return given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .body(fdcContribution)
                .patch(DCES_BASE_URL + FDC_CONTRIBUTION_URI)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);
    }

    public FdcContribution getFdcContribution(long fdcContributionId) {
        String contributionIdParameter = "/{fdcContributionId}";
        ValidatableResponse response =  given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .pathParam("fdcContributionId",fdcContributionId)
                .get(DCES_BASE_URL + FDC_CONTRIBUTION_URI+contributionIdParameter)
                .then()
                .log()
                .all();

        return response.extract().body().as(FdcContribution.class);
    }

    public ValidatableResponse updateRepOrder(UpdateRepOrder updateRepOrder) {
        return given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .body(updateRepOrder)
                .put(REP_ORDERS_BASE_URL)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);
    }

    public ContributionFileResponse getContributionFile(long contributionFileId) {
        String contributionIdUri = "/{contributionFileId}";
        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .pathParam("contributionFileId",contributionFileId)
                .get(DCES_BASE_URL + CONTRIBUTION_FILE_URI +contributionIdUri)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);
        return response.extract().body().as(ContributionFileResponse.class);
    }

    public List<Long> updateConcorContributionStatus(UpdateConcorContributionStatusRequest concorContributionRequest) {
        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .body(concorContributionRequest)
                .put(DCES_BASE_URL+ CONCOR_CONTRIBUTION_STATUS_URI)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);

        return response.extract().body().jsonPath().getList(".", Long.class);
    }

    public ConcorContributionResponseDTO getConcorContribution(long concorId) {
        String contributionIdUri = "/{concorId}";
        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .pathParam("concorId",concorId)
                .get(DCES_BASE_URL + CONCOR_CONTRIBUTION_URI +contributionIdUri)
                .then()
                .log()
                .all()
                .assertThat().statusCode(200);
        return response.extract().body().as(ConcorContributionResponseDTO.class);
    }

    public ContributionFileErrorResponse getContributionFileError(long contributionFileId, long contributionId) {
        String contributionFileIdUri = "/{contributionFileId}/error/{contributionId}";
        ValidatableResponse response = given()
                .spec(builder.getMaatAPICrimeApplyReqSpec())
                .pathParam("contributionFileId", contributionFileId)
                .pathParam("contributionId", contributionId)
                .get(DCES_BASE_URL + CONTRIBUTION_FILE_URI + contributionFileIdUri)
                .then()
                .log()
                .all();
        try {
            return response.extract().body().as(ContributionFileErrorResponse.class);
        }
        catch (RuntimeException e){
            return null;
        }

    }
}