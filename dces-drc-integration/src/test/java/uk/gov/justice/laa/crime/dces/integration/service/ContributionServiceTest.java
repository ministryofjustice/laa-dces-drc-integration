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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ProblemDetail;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ObjectFactory;
import uk.gov.justice.laa.crime.dces.integration.utils.ContributionsMapperUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FAILED_DEPENDENCY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SoftAssertionsExtension.class)
@WireMockTest(httpPort = 1111)
class ContributionServiceTest extends ApplicationTestBase {
    private static final String GET_URL = "/debt-collection-enforcement/concor-contribution-files?status=ACTIVE&concorContributionId=0&numberOfRecords=5";
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
	private FeatureProperties feature;

	@Autowired
	private ContributionService contributionService;

	@Value("${services.maat-api.getContributionBatchSize:0}")
	private int defaultGetContributionBatchSize;

	@MockBean
	private EventService eventService;

	private final Long testBatchId = -666L;

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
		when(eventService.generateBatchId()).thenReturn(testBatchId);
		when(contributionsMapperUtils.mapLineXMLToObject(any())).thenReturn(testContribution);
		when(contributionsMapperUtils.generateFileXML(any(), any())).thenReturn("ValidXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("ValidAckXML");
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
		verify(eventService).logConcor(null, EventType.FETCHED_FROM_MAAT, testBatchId, null, OK, "Fetched:5");
		verify(eventService).logConcor(1111L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(2222L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(3333L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(4444L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(5555L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(null, EventType.FETCHED_FROM_MAAT, testBatchId, null, OK, "Fetched:3");
		verify(eventService).logConcor(1000L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(6666L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(7777L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);

		verify(eventService).logConcor(1111L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(2222L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(3333L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(4444L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(5555L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(1000L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(6666L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(7777L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);

		verify(eventService).logConcor(1111L, EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(2222L, EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(3333L, EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(4444L, EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(5555L, EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(1000L, EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(6666L, EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(7777L, EventType.UPDATED_IN_MAAT, testBatchId, testContribution, OK, null);
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
	void testDrcUpdateWebClientConflictException() throws JAXBException {
		// setup
		ReflectionTestUtils.setField(contributionService, "getContributionBatchSize", 5);
		when(contributionsMapperUtils.mapLineXMLToObject(anyString())).thenReturn(createTestContribution());
		when(contributionsMapperUtils.generateFileXML(any(), anyString())).thenReturn("ValidXML");
		when(contributionsMapperUtils.generateFileName(any())).thenReturn("TestFilename.xml");
		when(contributionsMapperUtils.generateAckXML(anyString(), any(), intThat(n -> n != 0), anyInt())).thenThrow(new IllegalStateException("failed != 0"));
		when(contributionsMapperUtils.generateAckXML(anyString(), any(), eq(0), anyInt())).thenReturn("ValidAckXML");
		// Setting WebClientResponseException body & MIME type is not enough for `#getResponseAs(Class)` to work.
		// There's also a non-blocking `bodyDecodeFunction` set by `ClientResponse.createException()`/`createError()`.
		var problemDetail = ProblemDetail.forStatus(409);
		problemDetail.setType(URI.create("https://laa-debt-collection.service.justice.gov.uk/problem-types#duplicate-id"));
		var exception = new WebClientResponseException(409, "Conflict", null, null, null);
		exception.setBodyDecodeFunction(clazz -> clazz.isAssignableFrom(ProblemDetail.class) ? problemDetail : null);
		Mockito.doThrow(exception).when(drcClient).sendConcorContributionReqToDrc(any());
		// do
		boolean successful = contributionService.processDailyFiles();
		// test (asking for batch size of 5 makes WireMock return 8 records)
		softly.assertThat(successful).isTrue();
		verify(contributionsMapperUtils, times(8)).mapLineXMLToObject(any());
		verify(drcClient, times(8)).sendConcorContributionReqToDrc(any());
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
				.isInstanceOf(WebClientResponseException.class)
				.hasMessageContaining("500 Internal Server Error");
		verify(contributionsMapperUtils, times(2)).mapLineXMLToObject(any());
		// failure to generate the xml should return a null xmlString.
		verify(contributionsMapperUtils).generateFileXML(any(), any());
		// failure should be the result of file generation

		verify(eventService, times(6)).logConcor(any(), any(), any(), any(), any(), any());
		// verify each event is logged.
		verify(eventService).logConcor(null, EventType.FETCHED_FROM_MAAT, testBatchId, null, OK, "Fetched:2");
		verify(eventService).logConcor(1234L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(9876L, EventType.FETCHED_FROM_MAAT, testBatchId, testContribution, OK, null);

		verify(eventService).logConcor(1234L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);
		verify(eventService).logConcor(9876L, EventType.SENT_TO_DRC, testBatchId, testContribution, OK, null);

		verify(eventService).logConcor(null, EventType.UPDATED_IN_MAAT, testBatchId, null, INTERNAL_SERVER_ERROR, "Failed to create contribution-file: [500 Internal Server Error from POST http://localhost:1111/debt-collection-enforcement/create-contribution-file]");

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
				.isInstanceOf(WebClientResponseException.class);
		// test
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testProcessContributionUpdateWhenSuccessful() {
		ContributionProcessedRequest dataRequest = ContributionProcessedRequest.builder()
				.concorId(911L)
				.build();
		Long response = contributionService.handleContributionProcessedAck(dataRequest);
		softly.assertThat(response).isEqualTo(1111L);
		verify(eventService).logConcor(911L,EventType.DRC_ASYNC_RESPONSE,null,null, OK, null);
	}

	@Test
	void testProcessContributionUpdateWhenIncomingIsolated() {
		when(feature.incomingIsolated()).thenReturn(true);
		ContributionProcessedRequest dataRequest = ContributionProcessedRequest.builder()
				.concorId(911L)
				.build();
		Long response = contributionService.handleContributionProcessedAck(dataRequest);
		softly.assertThat(response).isEqualTo(0L); // so MAAT DB not touched
	}


	@Test
	void testProcessContributionUpdateWhenNotFound() {
		String errorText = "The request has failed to process";
		ContributionProcessedRequest dataRequest = ContributionProcessedRequest.builder()
				.concorId(404L)
				.errorText(errorText)
				.build();
		var exception = catchThrowableOfType(() -> contributionService.handleContributionProcessedAck(dataRequest), ErrorResponseException.class);
		softly.assertThat(exception).isNotNull();
		softly.assertThat(NOT_FOUND.isSameCodeAs(exception.getStatusCode())).isTrue();
		verify(eventService).logConcor(404L,EventType.DRC_ASYNC_RESPONSE,null,null, NOT_FOUND, errorText);
	}

	@Test
	void testProcessContributionUpdateWhenNoContFile() {
		String errorText = "The request has failed to process";
		ContributionProcessedRequest dataRequest = ContributionProcessedRequest.builder()
				.concorId(9L)
				.errorText(errorText)
				.build();
		var exception = catchThrowableOfType(() -> contributionService.handleContributionProcessedAck(dataRequest), ErrorResponseException.class);
		softly.assertThat(exception).isNotNull();
		softly.assertThat(FAILED_DEPENDENCY.isSameCodeAs(exception.getStatusCode())).isTrue();
		verify(eventService).logConcor(9L,EventType.DRC_ASYNC_RESPONSE,null,null, BAD_REQUEST, errorText);
	}

	CONTRIBUTIONS createTestContribution(){
		ObjectFactory of = new ObjectFactory();
		CONTRIBUTIONS cont = of.createCONTRIBUTIONS();
		cont.setMaatId(1111L);
		cont.setId(3333L);
		return cont;
	}
}
