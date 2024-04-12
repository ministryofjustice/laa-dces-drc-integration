package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ObjectFactory;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

	private static final List<StubMapping> customStubs = new ArrayList<>();

	private static final String GET_URL = "/debt-collection-enforcement/concor-contribution-files?status=ACTIVE";
	private static final String UPDATE_URL = "/debt-collection-enforcement/create-contribution-file";

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
		for(StubMapping stub: customStubs ){
			WireMock.removeStub(stub);
			WireMock.resetAllRequests();
		}
	}
	@Test
	void testXMLValid() throws JAXBException {

		when(contributionsMapperUtilsMock.mapLineXMLToObject(any())).thenReturn(createTestContribution());
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

	@Test
	void testAtomicUpdateFailure() throws JAXBException {
		// setup
		customStubs.add(stubFor(post(UPDATE_URL).atPriority(1)
				.willReturn(serverError())));

		when(contributionsMapperUtilsMock.mapLineXMLToObject(any())).thenReturn(createTestContribution());
		when(contributionsMapperUtilsMock.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtilsMock.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtilsMock.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
		// do
		Exception exception = Assert.assertThrows(HttpServerErrorException.class, () -> {
			contributionService.processDailyFiles();
		});
		// test
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testProcessContributionUpdateWhenReturnedTrue() {
		UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
				.concorId(911)
				.build();
		String response = contributionService.processContributionUpdate(dataRequest);
		assertEquals("The request has been processed successfully", response);
	}

	@Test
	void testProcessContributionUpdateWhenReturnedFalse() {
		String errorText = "The request has failed to process";
		UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
				.concorId(9)
				.errorText(errorText)
				.build();
		String response = contributionService.processContributionUpdate(dataRequest);
		assertEquals("The request has failed to process", response);
	}

	CONTRIBUTIONS createTestContribution(){
		ObjectFactory of = new ObjectFactory();
		CONTRIBUTIONS cont = of.createCONTRIBUTIONS();
		cont.setId(BigInteger.valueOf(3333));
		return cont;
	}
}
