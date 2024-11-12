package uk.gov.justice.laa.crime.dces.integration.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import javax.xml.datatype.DatatypeFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JacksonConfigurationTest extends ApplicationTestBase {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAConcorContributionReqForDrc_whenItIsSerialized_thenItHasNoNulls() throws JsonProcessingException {
        var request = ConcorContributionReqForDrc.of(123, new CONTRIBUTIONS(), null);
        String json = objectMapper.writeValueAsString(request);
        assertThat(json).doesNotContain("null");
    }

    @Test
    void givenAnFdcReqForDrc_whenItIsSerialized_thenItHasNoNulls() throws JsonProcessingException {
        var request = FdcReqForDrc.of(123, new FdcFile.FdcList.Fdc(), null);
        String json = objectMapper.writeValueAsString(request);
        assertThat(json).doesNotContain("null");
    }

    @Test
    void givenAnXMLGregorianCalendar_whenItIsSerialized_thenItHasIsoFormat() throws JsonProcessingException {
        var dateOnly = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2024-05-21");
        var timeOnly = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("14:49:35");
        var dateTime = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2024-05-21T14:49:35");
        assertThat(objectMapper.writeValueAsString(dateOnly)).isEqualTo("\"2024-05-21\"");
        assertThat(objectMapper.writeValueAsString(timeOnly)).isEqualTo("\"14:49:35\"");
        assertThat(objectMapper.writeValueAsString(dateTime)).isEqualTo("\"2024-05-21T14:49:35\"");
    }
}
