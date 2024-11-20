package uk.gov.justice.laa.crime.dces.integration.maatapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
@SpringBootTest
class ConcorContribEntryTest extends ApplicationTestBase {
    private static final Long DEFAULT_ID = 1L;
    private static final String DEFAULT_XML = "XML";


    @Test
    void givenMaatApiResponse_whenGetIdIsInvoked_thenCorrectIdIsReturned() {
        Long expectedId = 3L;
        ConcorContribEntry expectedResponse = new ConcorContribEntry(
                expectedId, DEFAULT_XML
        );
        assertThat(expectedResponse.getConcorContributionId()).isEqualTo(expectedId);
    }

    @Test
    void givenMaatApiResponse_whenGetTotalFilesIsInvoked_thenCorrectTotalFilesIsReturned() {
        String expectedXMLContent = "XML_TEST";
        ConcorContribEntry expectedResponse = new ConcorContribEntry(
                DEFAULT_ID, "XML_TEST"
        );
        assertThat(expectedResponse.getXmlContent()).isEqualTo(expectedXMLContent);
    }

    @Test
    void givenMaatApiResponse_whenSetTotalFilesIsInvoked_thenTotalFilesIsUpdated() {
        String expectedXMLContent = "XML_TEST_2";
        ConcorContribEntry response = new ConcorContribEntry(
                DEFAULT_ID, DEFAULT_XML
        );
        assertThat(response.getXmlContent()).isEqualTo(DEFAULT_XML);

        response.setXmlContent(expectedXMLContent);

        assertThat(response.getXmlContent()).isNotEqualTo(DEFAULT_XML);
        assertThat(response.getXmlContent()).isEqualTo(expectedXMLContent);
    }

    @Test
    void givenMaatApiResponse_whenSetIdIsInvoked_thenIdIsUpdated() {
        Long expectedId = 3L;
        ConcorContribEntry response = new ConcorContribEntry(
                DEFAULT_ID, DEFAULT_XML
        );
        assertThat(response.getConcorContributionId()).isEqualTo(DEFAULT_ID);

        response.setConcorContributionId(expectedId);

        assertThat(response.getConcorContributionId()).isNotEqualTo(DEFAULT_ID);
        assertThat(response.getConcorContributionId()).isEqualTo(expectedId);
    }

    @Test
    void givenMaatApiResponse_whenToStringInvoked_thenAStringIsReturned() {
        String expectedEstring = String.format("ConcorContribEntry(" +
                "concorContributionId=%s, xmlContent=%s)", DEFAULT_ID, DEFAULT_XML);
        ConcorContribEntry response = new ConcorContribEntry(
                DEFAULT_ID, DEFAULT_XML
        );

        assertThat(response.toString()).isInstanceOf(String.class);
        assertThat(response).hasToString(expectedEstring);
    }

}