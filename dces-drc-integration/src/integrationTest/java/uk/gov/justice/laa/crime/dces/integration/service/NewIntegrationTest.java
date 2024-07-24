package uk.gov.justice.laa.crime.dces.integration.service;

import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;

@Slf4j
@SpringBootTest
class NewIntegrationTest {

    @Autowired
    private MaatApiClient maatApiClient;

    @Autowired
    private FdcService fdcService;

    @Test
    void testFDCList_ApiClient() {

        ValidatableResponse response = maatApiClient.getFdcList("REQUESTED");

        response.assertThat().statusCode(200);
        response.body("fdcContributions", not(empty()));
    }
    @Test
    void testFdcFastTrack_ApiClient() {

        LocalDate date = LocalDate.of(2011,12,12);
        ValidatableResponse response = maatApiClient.getFdcFastTrackRepOrderIdList(5, date,5);

        response.assertThat().statusCode(200);
        response.body("", not(empty()));
    }
    @Test
    void testFdcDelay_ApiClient() {
        LocalDate date = LocalDate.of(2011,12,12);
        ValidatableResponse response = maatApiClient.getFdcDelayedRepOrderIdList(5, date,5);

        response.assertThat().statusCode(200);
        response.body("", not(empty()));

    }

    @Test
    void testFDCList_Service() {
        List<Fdc> fdcList = fdcService.getFdcList();
        log.info(fdcList.stream().map(x->x.getId().toString()).collect(Collectors.joining(",")));
    }


}