package uk.gov.justice.laa.crime.dces.integration.maatapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
@SpringBootTest
@ActiveProfiles("test")
class ConcurContribEntryTest {
    private static final int DEFAULT_ID = 1;
    private static final String DEFAULT_XML = "XML";


    @Test
    void givenMaatApiResponse_whenGetIdIsInvoked_thenCorrectIdIsReturned() {
        int expectedId = 3;
        ConcurContribEntry expectedResponse = new ConcurContribEntry(
                expectedId, DEFAULT_XML
        );
        assertThat(expectedResponse.getConcorContributionId()).isEqualTo(expectedId);
    }

    @Test
    void givenMaatApiResponse_whenGetTotalFilesIsInvoked_thenCorrectTotalFilesIsReturned() {
        String expectedXMLContent = "XML_TEST";
        ConcurContribEntry expectedResponse = new ConcurContribEntry(
                DEFAULT_ID, "XML_TEST"
        );
        assertThat(expectedResponse.getXmlContent()).isEqualTo(expectedXMLContent);
    }

    @Test
    void givenMaatApiResponse_whenSetTotalFilesIsInvoked_thenTotalFilesIsUpdated() {
        String expectedXMLContent = "XML_TEST_2";
        ConcurContribEntry response = new ConcurContribEntry(
                DEFAULT_ID, DEFAULT_XML
        );
        assertThat(response.getXmlContent()).isEqualTo(DEFAULT_XML);

        response.setXmlContent(expectedXMLContent);

        assertThat(response.getXmlContent()).isNotEqualTo(DEFAULT_XML);
        assertThat(response.getXmlContent()).isEqualTo(expectedXMLContent);
    }

    @Test
    void givenMaatApiResponse_whenSetIdIsInvoked_thenIdIsUpdated() {
        int expectedId = 3;
        ConcurContribEntry response = new ConcurContribEntry(
                DEFAULT_ID, DEFAULT_XML
        );
        assertThat(response.getConcorContributionId()).isEqualTo(DEFAULT_ID);

        response.setConcorContributionId(expectedId);

        assertThat(response.getConcorContributionId()).isNotEqualTo(DEFAULT_ID);
        assertThat(response.getConcorContributionId()).isEqualTo(expectedId);
    }

    @Test
    void givenMaatApiResponse_whenToStringInvoked_thenAStringIsReturned() {
        String expectedEstring = String.format("ConcurContribEntry(" +
                "id=%s, totalFiles=%s)", DEFAULT_ID, DEFAULT_XML);
        ConcurContribEntry response = new ConcurContribEntry(
                DEFAULT_ID, DEFAULT_XML
        );

        assertThat(response.toString()).isInstanceOf(String.class);
        assertThat(response).hasToString(expectedEstring);
    }

}