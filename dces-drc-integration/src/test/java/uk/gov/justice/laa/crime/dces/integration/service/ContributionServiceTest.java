package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest(httpPort = 1111)
class ContributionServiceTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@MockBean
	MapperUtils mapperUtilsMock;

	@Autowired
	private ContributionService contributionService;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testXMLValid() throws JAXBException {
		when(mapperUtilsMock.generateFileXML(any())).thenReturn("ValidXML");
		contributionService.processDailyFiles();
		verify(mapperUtilsMock,times(2)).mapLineXMLToObject(any());
		verify(mapperUtilsMock).generateFileXML(any());
	}

	@Test
	void testFileXMLInvalid() throws JAXBException {
		when(mapperUtilsMock.generateFileXML(any())).thenReturn(null);
		boolean result = contributionService.processDailyFiles();
		verify(mapperUtilsMock,times(2)).mapLineXMLToObject(any());
		// failure to generate the xml should return a null xmlString.
		verify(mapperUtilsMock).generateFileXML(any());
		// failure should be the result of file generation
		softly.assertThat(result).isFalse();
	}

	@Test
	void testLineXMLInvalid() throws JAXBException {
		when(mapperUtilsMock.mapLineXMLToObject(any())).thenThrow(JAXBException.class);
		contributionService.processDailyFiles();
		verify(mapperUtilsMock,times(2)).mapLineXMLToObject(any());
		// with no successful xml, should not run the file generation.
		verify(mapperUtilsMock, times(0)).generateFileXML(any());
	}

}
