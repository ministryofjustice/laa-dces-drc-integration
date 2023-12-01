package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@ActiveProfiles("test")
class ContributionServiceTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@InjectMocks
	private ContributionService contributionService;

	@Mock
	MapperUtils mapperUtilsMock;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testXMLValid() throws JAXBException {
		when(mapperUtilsMock.generateFileXML(any())).thenReturn("ValidXML");
		contributionService.processDailyFiles();
		verify(mapperUtilsMock).mapLineXMLToObject(any());
		verify(mapperUtilsMock).generateFileXML(any());
	}

	@Test
	void testFileXMLInvalid() throws JAXBException {
		when(mapperUtilsMock.generateFileXML(any())).thenReturn(null);
		boolean result = contributionService.processDailyFiles();
		verify(mapperUtilsMock).mapLineXMLToObject(any());
		// failure to generate the xml should return a null xmlString.
		verify(mapperUtilsMock).generateFileXML(any());
		// failure should be the result of file generation
		softly.assertThat(result).isFalse();
	}

	@Test
	void testLineXMLInvalid() throws JAXBException {
		when(mapperUtilsMock.mapLineXMLToObject(any())).thenThrow(JAXBException.class);
		contributionService.processDailyFiles();
		verify(mapperUtilsMock).mapLineXMLToObject(any());
		// with no successful xml, should not run the file generation.
		verify(mapperUtilsMock, Mockito.times(0)).generateFileXML(any());
	}

}
