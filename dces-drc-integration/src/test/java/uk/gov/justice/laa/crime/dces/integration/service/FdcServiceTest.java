package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FAILED_DEPENDENCY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(SoftAssertionsExtension.class)
@WireMockTest(httpPort = 1111)
class FdcServiceTest extends ApplicationTestBase {
	private static final String GET_URL = "/debt-collection-enforcement/fdc-contribution-files?status=REQUESTED";
	private static final String PREPARE_URL = "/debt-collection-enforcement/prepare-fdc-contributions-files";
	private static final String UPDATE_URL = "/debt-collection-enforcement/create-fdc-file";

	private static final List<StubMapping> customStubs = new ArrayList<>();

	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	@InjectMocks
	private FdcService fdcService;

	@MockBean
	FdcMapperUtils fdcMapperUtils;

	@MockBean
	private DrcClient drcClient;

	@MockBean
	private FeatureProperties feature;

	@MockBean
	private EventService eventService;

	private static final Long testBatchId = -666L;
	private static final String testDrcResponsePayload = "{\"meta\":{\"drcId\":12345,\"fdcId\":1234567}}";
	private static final String skipDrcResponsePayload = "{\"meta\":{\"drcId\":12345,\"fdcId\":1234567,\"skippedDueToFeatureOutgoingIsolated\":true}}";

	@Captor
	ArgumentCaptor<Fdc> fdcArgumentCaptor;

	@AfterEach
	void afterTestAssertAll() {
		softly.assertAll();
		for(StubMapping stub: customStubs ){
			WireMock.removeStub(stub);
		}
	}

	@Test
	void testXMLValid() {
		// setup
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.mapDRCJsonResponseToHttpStatus(anyString())).thenReturn(200);
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcReqToDrc(any())).thenReturn(testDrcResponsePayload);
		when(eventService.generateBatchId()).thenReturn(testBatchId);

		Fdc expectedFdc1 = createExpectedFdc(1000L, 10000000L, "2050-07-12", "2011-12-03", "3805.69","3805.69", "0");
		Fdc expectedFdc2 = createExpectedFdc(4000L, 40000000L, "2000-07-12", "2014-03-20", "2283.1","2283.1", "0");

		// run
		boolean successful = fdcService.processDailyFiles();
		// test
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils).generateFileName(any());
		verify(fdcMapperUtils).generateAckXML(any(), any(), any(), any());
		verify(fdcMapperUtils, times(12)).mapFdcEntry(any());
		verify(drcClient, times(12)).sendFdcReqToDrc(any());
		softly.assertThat(successful).isTrue();
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));


		// verify DB messages are being saved.
		// verify each event is logged, check for the DB calls.
		verify(eventService, times(12)).logFdc(eq(EventType.FETCHED_FROM_MAAT), eq(testBatchId), fdcArgumentCaptor.capture(), eq(HttpStatus.OK), eq(null));

		Fdc actualFdc1 = getFdcFromCaptorByFdcId(expectedFdc1.getId());
		assertFdcEquals(expectedFdc1, actualFdc1);
		Fdc actualFdc2 = getFdcFromCaptorByFdcId(expectedFdc2.getId());
		assertFdcEquals(expectedFdc2, actualFdc2);

		verify(eventService).logFdc(EventType.FETCHED_FROM_MAAT, testBatchId, actualFdc1, HttpStatus.OK, null);
		verify(eventService).logFdc(EventType.FETCHED_FROM_MAAT, testBatchId, actualFdc2, HttpStatus.OK, null);
		verify(eventService).logFdc(EventType.FETCHED_FROM_MAAT, testBatchId, null, HttpStatus.OK, "Fetched:12");

		verify(eventService).logFdc(EventType.SENT_TO_DRC, testBatchId, actualFdc1, HttpStatus.OK, testDrcResponsePayload);
		verify(eventService).logFdc(EventType.SENT_TO_DRC, testBatchId, actualFdc2, HttpStatus.OK, testDrcResponsePayload);
		verify(eventService, times(12)).logFdc(eq(EventType.SENT_TO_DRC), eq(testBatchId), any(), eq(HttpStatus.OK), eq(testDrcResponsePayload));

		verify(eventService).logFdc(EventType.FDC_GLOBAL_UPDATE, testBatchId, null, HttpStatus.OK, "Updated:3");

		verify(eventService).logFdc(EventType.UPDATED_IN_MAAT, testBatchId, actualFdc1, HttpStatus.OK, null);
		verify(eventService).logFdc(EventType.UPDATED_IN_MAAT, testBatchId, actualFdc2, HttpStatus.OK, null);
		verify(eventService, times(12)).logFdc(eq(EventType.UPDATED_IN_MAAT), eq(testBatchId), any(), eq(HttpStatus.OK), eq(null));
		verify(eventService).logFdc(EventType.UPDATED_IN_MAAT, testBatchId, null, HttpStatus.OK, "Successfully Sent:12");

	}

	@Test
	void testXMLValidWhenOutgoingIsolated() {
		// setup
		when(feature.outgoingIsolated()).thenReturn(true);
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.mapDRCJsonResponseToHttpStatus(anyString())).thenReturn(632);
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcReqToDrc(any())).thenReturn(skipDrcResponsePayload);
		// run
		boolean successful = fdcService.processDailyFiles();
		// test
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils).generateFileName(any());
		verify(fdcMapperUtils).generateAckXML(any(), any(), any(), any());
		verify(fdcMapperUtils, times(12)).mapFdcEntry(any());
		verify(drcClient, times(0)).sendFdcReqToDrc(any()); // nothing sent to DRC
		softly.assertThat(successful).isTrue();
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(0, postRequestedFor(urlEqualTo(PREPARE_URL))); // no FDC global update
		WireMock.verify(0, postRequestedFor(urlEqualTo(UPDATE_URL))); // no changes to statuses or contribution_files
	}

	@Test
	void testXMLValidWhenResponsePayloadEmpty() {
		// setup
		when(feature.outgoingIsolated()).thenReturn(false);
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.mapDRCJsonResponseToHttpStatus(anyString())).thenReturn(635);
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcReqToDrc(any())).thenReturn("");
		// run
		boolean successful = fdcService.processDailyFiles();
		// test
		verify(fdcMapperUtils, times(0)).generateFileXML(any());
		verify(fdcMapperUtils, times(0)).generateFileName(any());
		verify(fdcMapperUtils, times(0)).generateAckXML(any(), any(), any(), any());
		verify(fdcMapperUtils, times(12)).mapFdcEntry(any());
		verify(drcClient, times(12)).sendFdcReqToDrc(any());
		softly.assertThat(successful).isFalse();
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(0, postRequestedFor(urlEqualTo(UPDATE_URL))); // no changes to statuses or contribution_files
	}

	@Test
	void testFdcGlobalUpdateError() {
		// setup
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.mapDRCJsonResponseToHttpStatus(anyString())).thenReturn(200);
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcReqToDrc(any())).thenReturn(testDrcResponsePayload);
		when(eventService.generateBatchId()).thenReturn(testBatchId);
		customStubs.add(stubFor(post(PREPARE_URL).atPriority(1)
				.willReturn(serverError())));

		Fdc expectedFdc1 = createExpectedFdc(1000L, 10000000L, "2050-07-12", "2011-12-03", "3805.69","3805.69", "0");
		Fdc expectedFdc2 = createExpectedFdc(4000L, 40000000L, "2000-07-12", "2014-03-20", "2283.1","2283.1", "0");


		// run
		boolean successful = fdcService.processDailyFiles();

		// test
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils).generateFileName(any());
		verify(fdcMapperUtils).generateAckXML(any(),any(),any(),any());
		verify(fdcMapperUtils,times(12)).mapFdcEntry(any());
		softly.assertThat(successful).isTrue();
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));



		// verify DB messages are being saved.
		// verify each event is logged, check for the DB calls.
		verify(eventService, times(12)).logFdc(eq(EventType.FETCHED_FROM_MAAT), eq(testBatchId), fdcArgumentCaptor.capture(), eq(HttpStatus.OK), eq(null));

		Fdc actualFdc1 = getFdcFromCaptorByFdcId(expectedFdc1.getId());
		assertFdcEquals(expectedFdc1, actualFdc1);
		Fdc actualFdc2 = getFdcFromCaptorByFdcId(expectedFdc2.getId());
		assertFdcEquals(expectedFdc2, actualFdc2);

		verify(eventService).logFdc(EventType.FETCHED_FROM_MAAT, testBatchId, actualFdc1, HttpStatus.OK, null);
		verify(eventService).logFdc(EventType.FETCHED_FROM_MAAT, testBatchId, actualFdc2, HttpStatus.OK, null);
		verify(eventService).logFdc(EventType.FETCHED_FROM_MAAT, testBatchId, null, HttpStatus.OK, "Fetched:12");

		verify(eventService).logFdc(EventType.FDC_GLOBAL_UPDATE, testBatchId, null, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to complete FDC global update [500 Internal Server Error from POST http://localhost:1111/debt-collection-enforcement/prepare-fdc-contributions-files]");

		verify(eventService).logFdc(EventType.SENT_TO_DRC, testBatchId, actualFdc1, HttpStatus.OK, testDrcResponsePayload);
		verify(eventService).logFdc(EventType.SENT_TO_DRC, testBatchId, actualFdc2, HttpStatus.OK, testDrcResponsePayload);
		verify(eventService, times(12)).logFdc(eq(EventType.SENT_TO_DRC), eq(testBatchId), any(), eq(HttpStatus.OK), eq(testDrcResponsePayload));

		verify(eventService).logFdc(EventType.UPDATED_IN_MAAT, testBatchId, actualFdc1, HttpStatus.OK, null);
		verify(eventService).logFdc(EventType.UPDATED_IN_MAAT, testBatchId, actualFdc2, HttpStatus.OK, null);
		verify(eventService, times(12)).logFdc(eq(EventType.UPDATED_IN_MAAT), eq(testBatchId), any(), eq(HttpStatus.OK), eq(null));
		verify(eventService).logFdc(EventType.UPDATED_IN_MAAT, testBatchId, null, HttpStatus.OK,"Successfully Sent:12");
	}

	@Test
	void testFdcGlobalUpdateFailure() {
		// setup
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.mapDRCJsonResponseToHttpStatus(anyString())).thenReturn(200);
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcReqToDrc(any())).thenReturn(testDrcResponsePayload);
		customStubs.add(stubFor(post(PREPARE_URL).atPriority(1)
				.willReturn(serverError())));
		// run
		boolean successful = fdcService.processDailyFiles();


		// test
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils).generateFileName(any());
		verify(fdcMapperUtils).generateAckXML(any(),any(),any(),any());
		verify(fdcMapperUtils,times(12)).mapFdcEntry(any());
		softly.assertThat(successful).isTrue();
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testFdcGlobalUpdateFailureUnauthorised() {
		// setup
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.mapDRCJsonResponseToHttpStatus(anyString())).thenReturn(200);
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcReqToDrc(any())).thenReturn(testDrcResponsePayload);
		customStubs.add(stubFor(post(PREPARE_URL).atPriority(1)
				.willReturn(unauthorized())));
		// run
		boolean successful = fdcService.processDailyFiles();


		// test
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils).generateFileName(any());
		verify(fdcMapperUtils).generateAckXML(any(),any(),any(),any());
		verify(fdcMapperUtils,times(12)).mapFdcEntry(any());
		softly.assertThat(successful).isTrue();
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testGetFdcError() {
		// setup
		customStubs.add(stubFor(get(GET_URL).atPriority(1)
				.willReturn(serverError())));
		// do
		softly.assertThatThrownBy(() -> fdcService.processDailyFiles())
				.isInstanceOf(WebClientResponseException.class);
		// test
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(0, getRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testGetFdcNoResults() {
		// setup
		when(fdcMapperUtils.generateFileXML(any())).thenReturn(null);
		customStubs.add(stubFor(get(GET_URL).atPriority(1)
				.willReturn(ok("""
						{
							"fdcContributions": []
						}
						""").withHeader("Content-Type", "application/json"))));
		// do
		boolean successful = fdcService.processDailyFiles();
		// test
		softly.assertThat(successful).isFalse();
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(0, getRequestedFor(urlEqualTo(UPDATE_URL)));
		verify(fdcMapperUtils,times(0)).generateFileXML(any());
	}

	@Test
	void testDrcUpdateUnauthorised() {
		// setup
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		Mockito.doThrow(new WebClientResponseException(401, "Test Unauthorised", null, null, null)).when(drcClient).sendFdcReqToDrc(any());
		// do
		boolean successful = fdcService.processDailyFiles();
		// test
		softly.assertThat(successful).isFalse();
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		verify(drcClient, times(12)).sendFdcReqToDrc(any());
		WireMock.verify(0, getRequestedFor(urlEqualTo(UPDATE_URL)));
		verify(fdcMapperUtils,times(0)).generateFileXML(any());
	}
	@Test
	void testDrcUpdateInternalServerError() {
		// setup
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		Mockito.doThrow(new WebClientResponseException(500, "Test Unauthorised", null, null, null)).when(drcClient).sendFdcReqToDrc(any());
		// do
		boolean successful = fdcService.processDailyFiles();
		// test
		softly.assertThat(successful).isFalse();
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		verify(drcClient, times(12)).sendFdcReqToDrc(any());
		WireMock.verify(0, getRequestedFor(urlEqualTo(UPDATE_URL)));
		verify(fdcMapperUtils,times(0)).generateFileXML(any());
	}
	@Test
	void testDrcUpdateWebClientException() {
		// setup
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		Mockito.doThrow(new WebClientResponseException(403,"Failed", null, null, null)).when(drcClient).sendFdcReqToDrc(any());
		// do
		boolean successful = fdcService.processDailyFiles();
		// test
		softly.assertThat(successful).isFalse();
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		verify(drcClient, times(12)).sendFdcReqToDrc(any());
		WireMock.verify(0, postRequestedFor(urlEqualTo(UPDATE_URL)));
		verify(fdcMapperUtils,times(0)).generateFileXML(any());
	}

	@Test
	void testDrcUpdateWebClientConflictException() {
		// setup
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(anyString(), any(), intThat(n -> n != 0), anyInt())).thenThrow(new IllegalStateException("failed != 0"));
		when(fdcMapperUtils.generateAckXML(anyString(), any(), eq(0), anyInt())).thenReturn("<xml>ValidAckXML</xml>");
		// Setting WebClientResponseException body & MIME type is not enough for `#getResponseAs(Class)` to work.
		// There's also a non-blocking `bodyDecodeFunction` set by `ClientResponse.createException()`/`createError()`.
		var problemDetail = ProblemDetail.forStatus(409);
		problemDetail.setType(URI.create("https://laa-debt-collection.service.justice.gov.uk/problem-types#duplicate-id"));
		var exception = new WebClientResponseException(409, "Conflict", null, null, null);
		exception.setBodyDecodeFunction(clazz -> clazz.isAssignableFrom(ProblemDetail.class) ? problemDetail : null);
		Mockito.doThrow(exception).when(drcClient).sendFdcReqToDrc(any());
		// do
		boolean successful = fdcService.processDailyFiles();
		// test
		softly.assertThat(successful).isTrue();
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		verify(drcClient, times(12)).sendFdcReqToDrc(any());
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
		verify(fdcMapperUtils,times(1)).generateFileXML(any());
	}

	@Test
	void testAtomicUpdateFailure(){
		// setup
		customStubs.add(stubFor(post(UPDATE_URL).atPriority(1)
				.willReturn(serverError())));

		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.mapDRCJsonResponseToHttpStatus(anyString())).thenReturn(200);
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcReqToDrc(any())).thenReturn(testDrcResponsePayload);

		// do
		softly.assertThatThrownBy(() -> fdcService.processDailyFiles())
				.isInstanceOf(WebClientResponseException.class);
		// test
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testProcessFdcUpdateWhenSuccessful() {
		FdcProcessedRequest dataRequest = FdcProcessedRequest.builder()
				.fdcId(911L)
				.build();
		Long response = fdcService.handleFdcProcessedAck(dataRequest);
		softly.assertThat(response).isEqualTo(1111L);
	}

	@Test
	void testProcessFdcUpdateWhenIncomingIsolated() {
		when(feature.incomingIsolated()).thenReturn(true);
		FdcProcessedRequest dataRequest = FdcProcessedRequest.builder()
				.fdcId(911L)
				.build();
		Long response = fdcService.handleFdcProcessedAck(dataRequest);
		softly.assertThat(response).isEqualTo(0L); // so MAAT DB not touched
	}

	@Test
	void testProcessFdcUpdateWhenNotFound() {
		String errorText = "The request has failed to process";
		FdcProcessedRequest dataRequest = FdcProcessedRequest.builder()
				.fdcId(404L)
				.errorText(errorText)
				.build();
		var exception = catchThrowableOfType(() -> fdcService.handleFdcProcessedAck(dataRequest), ErrorResponseException.class);
		softly.assertThat(exception).isNotNull();
		softly.assertThat(NOT_FOUND.isSameCodeAs(exception.getStatusCode())).isTrue();
	}

	@Test
	void testProcessFdcUpdateWhenNoContFile() {
		String errorText = "The request has failed to process";
		FdcProcessedRequest dataRequest = FdcProcessedRequest.builder()
				.fdcId(9L)
				.errorText(errorText)
				.build();
		var exception = catchThrowableOfType(() -> fdcService.handleFdcProcessedAck(dataRequest), ErrorResponseException.class);
		softly.assertThat(exception).isNotNull();
		softly.assertThat(FAILED_DEPENDENCY.isSameCodeAs(exception.getStatusCode())).isTrue();
	}

	@Test
	void testProcessFdcUpdateWhenServerFailure() {
		String errorText = "The request has failed to process";
		FdcProcessedRequest dataRequest = FdcProcessedRequest.builder()
				.fdcId(500L)
				.errorText(errorText)
				.build();
		var exception = catchThrowableOfType(() -> fdcService.handleFdcProcessedAck(dataRequest), ErrorResponseException.class);
		softly.assertThat(exception).isNotNull();
		softly.assertThat(exception.getStatusCode().is5xxServerError()).isTrue();
	}

	@Test
	void givenIdList_whenSendFdcsToDrcIsCalled_thenXmlIsFetchedAndSent() {
		when(feature.outgoingIsolated()).thenReturn(false);
		Fdc testFdc = createExpectedFdc(1000L, 10000000L, "2050-07-12", "2011-12-03", "3805.69","3805.69", "0");

		when(fdcMapperUtils.mapFdcEntry(any())).thenReturn(testFdc);
		when(fdcMapperUtils.mapDRCJsonResponseToHttpStatus(anyString())).thenReturn(200);
		when(drcClient.sendFdcReqToDrc(any())).thenReturn(testDrcResponsePayload);

		List<Fdc> result = fdcService.sendFdcsToDrc(List.of(1L, 2L));
		verify(fdcMapperUtils, times(2)).mapFdcEntry(any());
		verify(drcClient, times(2)).sendFdcReqToDrc(any());
		verify(fdcMapperUtils, never()).generateFileXML(any()); // verify that no file is generated
		softly.assertThat(result).hasSize(2);
		softly.assertThat(result.get(0).getId()).isEqualTo(1000L);
		softly.assertThat(result.get(0).getMaatId()).isEqualTo(10000000L);
		softly.assertThat(result.get(1).getFinalCost()).isEqualByComparingTo("3805.69");
	}

	@Test
	void givenEmptyIdList_whenSendContributionsToDrcIsCalled_thenErrorIsReturned() {
		when(feature.outgoingIsolated()).thenReturn(false);
		Fdc testFdc = createExpectedFdc(1000L, 10000000L, "2050-07-12", "2011-12-03", "3805.69","3805.69", "0");
		when(fdcMapperUtils.mapFdcEntry(any())).thenReturn(testFdc);
		when(drcClient.sendFdcReqToDrc(any())).thenReturn(testDrcResponsePayload);
		softly.assertThatThrownBy(() -> fdcService.sendFdcsToDrc(List.of()))
				.isInstanceOf(BadRequest.class)
				.hasMessageContaining("400 Bad Request");
		verify(fdcMapperUtils, never()).mapFdcEntry(any());
		verify(drcClient, never()).sendFdcReqToDrc(any());
	}

	Fdc createExpectedFdc(Long id, Long maatId, String sentenceDate, String calculationDate, String finalCost, String lgfsTotal, String agfsTotal){
		Fdc fdc = new Fdc();
		fdc.setId(id);
		fdc.setMaatId(maatId);
		fdc.setSentenceDate(LocalDate.parse(sentenceDate));
		fdc.setCalculationDate(LocalDate.parse(calculationDate));
		fdc.setFinalCost(new BigDecimal(finalCost));
		fdc.setLgfsTotal(new BigDecimal(lgfsTotal));
		fdc.setAgfsTotal(new BigDecimal(agfsTotal));
		return fdc;
	}

	private void assertFdcEquals(Fdc fdc1, Fdc fdc2){
		softly.assertThat(fdc1.getId()).isEqualTo(fdc2.getId());
		softly.assertThat(fdc1.getMaatId()).isEqualTo(fdc2.getMaatId());
		softly.assertThat(fdc1.getAgfsTotal()).isEqualTo(fdc2.getAgfsTotal());
		softly.assertThat(fdc1.getLgfsTotal()).isEqualTo(fdc2.getLgfsTotal());
		softly.assertThat(fdc1.getFinalCost()).isEqualTo(fdc2.getFinalCost());
		softly.assertThat(fdc1.getSentenceDate()).isEqualTo(fdc2.getSentenceDate());
		softly.assertThat(fdc1.getCalculationDate()).isEqualTo(fdc2.getCalculationDate());
	}

	private Fdc getFdcFromCaptorByFdcId(Long fdcId){
		Optional<Fdc> optionalFdc =  fdcArgumentCaptor.getAllValues().stream().filter(x-> x.getId().equals(fdcId)).findFirst();
		softly.assertThat(optionalFdc.isPresent()).isTrue();
		return optionalFdc.get();
	}

}
