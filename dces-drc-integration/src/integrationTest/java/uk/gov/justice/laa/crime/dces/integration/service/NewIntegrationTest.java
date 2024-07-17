package uk.gov.justice.laa.crime.dces.integration.service;

import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;

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
    void testFDCList_Service() {

        List<Fdc> fdcList = fdcService.getFdcList();
        System.out.println(fdcList);
    }


}