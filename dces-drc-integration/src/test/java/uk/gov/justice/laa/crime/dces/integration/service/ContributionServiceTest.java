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
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;
import org.springframework.web.client.HttpServerErrorException;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest(httpPort = 1111)
class ContributionServiceTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@MockBean
	ContributionsMapperUtils contributionsMapperUtilsMock;

	@Autowired
	private ContributionService contributionService;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testXMLValid() throws JAXBException {
		when(contributionsMapperUtilsMock.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtilsMock.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtilsMock.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
		boolean result = contributionService.processDailyFiles();
		verify(contributionsMapperUtilsMock,times(2)).mapLineXMLToObject(any());
		verify(contributionsMapperUtilsMock).generateFileXML(any(), any());
		softly.assertThat(result).isTrue();
	}

	@Test
	void testFileXMLInvalid() throws JAXBException {
		when(contributionsMapperUtilsMock.generateFileXML(any(), any())).thenReturn("InvalidXML");
		when(contributionsMapperUtilsMock.generateAckXML(any(),any(),any(),any())).thenReturn("AckXML");
		when(contributionsMapperUtilsMock.generateFileName(any())).thenReturn("FileName");


		Exception exception = assertThrows(HttpServerErrorException.class, () -> {
			contributionService.processDailyFiles();
		});
		verify(contributionsMapperUtilsMock,times(2)).mapLineXMLToObject(any());
		// failure to generate the xml should return a null xmlString.
		verify(contributionsMapperUtilsMock).generateFileXML(any(), any());
		// failure should be the result of file generation

		String expectedMessage = "500 Received error 500 INTERNAL_SERVER_ERROR due to Internal Server Error";
		String actualMessage = exception.getMessage();

		softly.assertThat(actualMessage.contains(expectedMessage)).isTrue();
	}

	@Test
	void testLineXMLInvalid() throws JAXBException {
		when(contributionsMapperUtilsMock.mapLineXMLToObject(any())).thenThrow(JAXBException.class);
		contributionService.processDailyFiles();
		verify(contributionsMapperUtilsMock,times(2)).mapLineXMLToObject(any());
		// with no successful xml, should not run the file generation.
		verify(contributionsMapperUtilsMock, times(0)).generateFileXML(any(), any());
	}
}
