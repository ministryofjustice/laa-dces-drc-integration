package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.Feature;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ObjectFactory;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(SoftAssertionsExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest(httpPort = 1111)
class ContributionServiceTest {
	private static final String GET_URL = "/debt-collection-enforcement/concor-contribution-files?status=ACTIVE&startingId=0&numberOfRecords=5";
	private static final String UPDATE_URL = "/debt-collection-enforcement/create-contribution-file";

	private static final List<StubMapping> customStubs = new ArrayList<>();

	@InjectSoftAssertions
	private SoftAssertions softly;

	@MockBean
	private ContributionsMapperUtils contributionsMapperUtils;

	@MockBean
	private AnonymisingDataService anonymisingDataService;

	@MockBean
	private DrcClient drcClient;

	@MockBean
	private Feature feature;

	@Autowired
	private ContributionService contributionService;

	@Value("${services.maat-api.getContributionBatchSize:0}")
	private int defaultGetContributionBatchSize;

	@MockBean
	private EventService eventService;

	private final BigInteger testBatchId = BigInteger.valueOf(-666);

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
		for (StubMapping stub: customStubs) {
			WireMock.removeStub(stub);
			WireMock.resetAllRequests();
		}
	}

	@BeforeEach
	void setGetContributionSize(){
		ReflectionTestUtils.setField(contributionService, "getContributionBatchSize", defaultGetContributionBatchSize);
	}

	@SuppressWarnings("squid:S5961") // suppress the "too many asserts" error. Asserting just the
									 // right things have been logged is over the max of 25 already.
	@Test
	void testXMLValid() throws JAXBException {
		ReflectionTestUtils.setField(contributionService, "getContributionBatchSize", 5);
		CONTRIBUTIONS testContribution = createTestContribution();
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenReturn(testContribution);
		when(contributionsMapperUtils.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
		when(eventService.generateBatchId()).thenReturn(testBatchId);
		doNothing().when(drcClient).sendConcorContributionReqToDrc(any());

		boolean result = contributionService.processDailyFiles();
		//testing the number of times the methods are called to ensure the correct number of records are processed.
		verify(contributionsMapperUtils, times(8)).mapLineXMLToObject(any());
		verify(contributionsMapperUtils, times(2)).generateFileXML(any(), any());
		verify(drcClient, times(8)).sendConcorContributionReqToDrc(any());
		softly.assertThat(result).isTrue();
		verify(anonymisingDataService, never()).anonymise(any());

		verify(eventService, times(30)).logConcor(any(), any(), any(), any(), any(), any());
		// verify each event is logged.
		verify(eventService).logConcor(null, EventType.FETCHED_FROM_MAAT, testBatchId, null, OK, "Fetched 5 concorContribution entries");
		verify(eventService).logConcor(BigInteger.valueOf(1111), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(2222), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(3333), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(4444), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(5555), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(null, EventType.FETCHED_FROM_MAAT, testBatchId, null, OK, "Fetched 3 concorContribution entries");
		verify(eventService).logConcor(BigInteger.valueOf(1000), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(6666), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(7777), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);

		verify(eventService).logConcor(BigInteger.valueOf(1111), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(2222), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(3333), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(4444), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(5555), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(1000), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(6666), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(7777), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);

		verify(eventService).logConcor(BigInteger.valueOf(1111), EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(2222), EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(3333), EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(4444), EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(5555), EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(1000), EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(6666), EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(7777), EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(null, EventType.UPDATED_IN_MAAT, testBatchId, null, OK, "Successfully Sent:5");
		verify(eventService).logConcor(null, EventType.UPDATED_IN_MAAT, testBatchId, null, OK, "Successfully Sent:3");
		verify(eventService, times(2)).logConcor(null, EventType.UPDATED_IN_MAAT, testBatchId, null, OK, "Failed To Send:0");

	}

	@Test
	void testWhenContributionHasFewerRecords() throws JAXBException {
		// set wanted number to trigger the correct stub response.
		ReflectionTestUtils.setField(contributionService, "getContributionBatchSize", 4);
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenReturn(createTestContribution());
		when(contributionsMapperUtils.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
		doNothing().when(drcClient).sendConcorContributionReqToDrc(any());

		boolean result = contributionService.processDailyFiles();
		//testing the number of times the methods are called to ensure the correct number of records are processed.
		verify(contributionsMapperUtils, times(3)).mapLineXMLToObject(any());
		verify(contributionsMapperUtils, times(1)).generateFileXML(any(), any());
		verify(drcClient, times(3)).sendConcorContributionReqToDrc(any());
		softly.assertThat(result).isTrue();
		verify(anonymisingDataService, never()).anonymise(any());
	}

	@Test
	void testXMLValidWhenOutgoingAnonymizedFlagIsFalse() throws JAXBException {
		ReflectionTestUtils.setField(contributionService, "getContributionBatchSize", 5);
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenReturn(createTestContribution());
		when(contributionsMapperUtils.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
		doNothing().when(drcClient).sendConcorContributionReqToDrc(any());
		when(feature.outgoingAnonymized()).thenReturn(false);

		contributionService.processDailyFiles();

		verify(anonymisingDataService, never()).anonymise(any());
	}

	@Test
	void testXMLValidWhenOutgoingAnonymizedFlagIsTrue() throws JAXBException {
		CONTRIBUTIONS contributions = createTestContribution();
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenReturn(contributions);
		when(contributionsMapperUtils.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
		doNothing().when(drcClient).sendConcorContributionReqToDrc(any());
		when(feature.outgoingAnonymized()).thenReturn(true);

		contributionService.processDailyFiles();

		verify(anonymisingDataService, times(2)).anonymise(any());
	}


	@Test
	void testXMLValidWhenOutgoingIsolated() throws JAXBException {
		when(feature.outgoingIsolated()).thenReturn(true);
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenReturn(createTestContribution());
		when(contributionsMapperUtils.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
		doNothing().when(drcClient).sendConcorContributionReqToDrc(any());

		boolean result = contributionService.processDailyFiles();
		verify(contributionsMapperUtils, times(2)).mapLineXMLToObject(any());
		verify(contributionsMapperUtils).generateFileXML(any(), any());
		verify(drcClient, times(0)).sendConcorContributionReqToDrc(any()); // not called when feature.outgoing-isolated=true.
		softly.assertThat(result).isTrue();
	}

	@Test
	void testFileXMLInvalid() throws JAXBException {
		CONTRIBUTIONS testContribution = createTestContribution();
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenReturn(testContribution); // mock returns null otherwise
		when(contributionsMapperUtils.generateFileXML(any(), any())).thenReturn("InvalidXML");
		when(contributionsMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("AckXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("FileName");
		doNothing().when(drcClient).sendConcorContributionReqToDrc(any());
		when(eventService.generateBatchId()).thenReturn(testBatchId);

		softly.assertThatThrownBy(() -> contributionService.processDailyFiles())
				.isInstanceOf(HttpServerErrorException.class)
				.hasMessageContaining("INTERNAL_SERVER_ERROR");
		verify(contributionsMapperUtils, times(2)).mapLineXMLToObject(any());
		// failure to generate the xml should return a null xmlString.
		verify(contributionsMapperUtils).generateFileXML(any(), any());
		// failure should be the result of file generation

		verify(eventService, times(6)).logConcor(any(), any(), any(), any(), any(), any());
		// verify each event is logged.
		verify(eventService).logConcor(null, EventType.FETCHED_FROM_MAAT, testBatchId, null, OK, "Fetched 2 concorContribution entries");
		verify(eventService).logConcor(BigInteger.valueOf(1234), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(9876), EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);

		verify(eventService).logConcor(BigInteger.valueOf(1234), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(BigInteger.valueOf(9876), EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);

		verify(eventService).logConcor(null, EventType.UPDATED_IN_MAAT, testBatchId, null, INTERNAL_SERVER_ERROR, "Failed to create Concor contribution-file. Investigation needed. State of files will be out of sync! [org.springframework.web.client.HttpServerErrorException(500 Received error 500 INTERNAL_SERVER_ERROR due to Internal Server Error)]");

	}

	@Test
	void testLineXMLInvalid() throws JAXBException {
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenThrow(JAXBException.class);
		contributionService.processDailyFiles();
		verify(contributionsMapperUtils, times(2)).mapLineXMLToObject(any());
		// with no successful xml, should not run the file generation.
		verify(contributionsMapperUtils, times(0)).generateFileXML(any(), any());
	}

	@Test
	void testAtomicUpdateFailure() throws JAXBException {
		// setup
		customStubs.add(stubFor(post(UPDATE_URL).atPriority(1)
				.willReturn(serverError())));
		ReflectionTestUtils.setField(contributionService, "getContributionBatchSize", 5);
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenReturn(createTestContribution());
		when(contributionsMapperUtils.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
		doNothing().when(drcClient).sendConcorContributionReqToDrc(any());
		// do
		softly.assertThatThrownBy(() -> contributionService.processDailyFiles())
				.isInstanceOf(HttpServerErrorException.class);
		// test
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testProcessContributionUpdateWhenSuccessful() {
		UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
				.concorId(911)
				.build();
		Integer response = contributionService.processContributionUpdate(dataRequest);
		softly.assertThat(response).isEqualTo(1111);
		verify(eventService).logConcor(BigInteger.valueOf(911),EventType.DRC_ASYNC_RESPONSE,null,null, OK, null);
	}

	@Test
	void testProcessContributionUpdateWhenIncomingIsolated() {
		when(feature.incomingIsolated()).thenReturn(true);
		UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
				.concorId(911)
				.build();
		Integer response = contributionService.processContributionUpdate(dataRequest);
		softly.assertThat(response).isEqualTo(0); // so MAAT DB not touched
	}


	@Test
	void testProcessContributionUpdateWhenFailed() {
		String errorText = "The request has failed to process";
		UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
				.concorId(9)
				.errorText(errorText)
				.build();
		var exception = catchThrowableOfType(() -> contributionService.processContributionUpdate(dataRequest), MaatApiClientException.class);
		softly.assertThat(exception).isNotNull();
		softly.assertThat(exception.getStatusCode().is4xxClientError()).isTrue();
		verify(eventService).logConcor(BigInteger.valueOf(9),EventType.DRC_ASYNC_RESPONSE,null,null, BAD_REQUEST, errorText);
	}

	CONTRIBUTIONS createTestContribution(){
		ObjectFactory of = new ObjectFactory();
		CONTRIBUTIONS cont = of.createCONTRIBUTIONS();
		cont.setMaatId(BigInteger.valueOf(1111));
		cont.setId(BigInteger.valueOf(3333));
		return cont;
	}
}
