package uk.gov.justice.laa.crime.dces.integration.testing;

import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class NewIntegrationTest {

    @Autowired
    private MaatApiClient maatApiClient;

    @Autowired
    private FdcService fdcService;

    @Test
    void testFDCList_ApiClient() {

        List<FdcContribution> response = maatApiClient.getFdcList("REQUESTED");

        assertFalse(response.isEmpty());
    }
    @Test
    void testFdcFastTrack_ApiClient() {

        LocalDate date = LocalDate.of(2011,12,12);
        Set<Integer> response = maatApiClient.getFdcFastTrackRepOrderIdList(5, date,5);
        assertEquals(5,response.size());
    }

    @Test
    void testGetFdcContribution() {
        FdcContribution contribution = maatApiClient.getFdcContribution(5);
        assertTrue(Objects.nonNull(contribution));
    }

    @Test
    void testFdcDelay_ApiClient() {
        LocalDate date = LocalDate.of(2011,12,12);
        Set<Integer> response = maatApiClient.getFdcDelayedRepOrderIdList(5, date,5);
        assertEquals(5,response.size());

    }

    @Test
    void testFDCList_Service() {
        List<Fdc> fdcList = fdcService.getFdcList();
        log.info(fdcList.stream().map(x->x.getId().toString()).collect(Collectors.joining(",")));
    }




}