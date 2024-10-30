package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.Feature;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static uk.gov.justice.laa.crime.dces.integration.utils.DateConvertor.convertToXMLGregorianCalendar;

@ExtendWith(SoftAssertionsExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest(httpPort = 1111)
class FdcServiceTest {
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
	private Feature feature;

	@MockBean
	private EventService eventService;

	private final BigInteger testBatchId = BigInteger.valueOf(-666);

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
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("<xml>ValidAckXML</xml>");
		doNothing().when(drcClient).sendFdcReqToDrc(any());
		when(eventService.generateBatchId()).thenReturn(testBatchId);

		Fdc expectedFdc1 = createExpectedFdc(1000, 10000000, "2050-07-12", "2011-12-03", "3805.69","3805.69", "0");
		Fdc expectedFdc2 = createExpectedFdc(4000, 40000000, "2000-07-12", "2014-03-20", "2283.1","2283.1", "0");

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
		verify(eventService).logFdc(EventType.FETCHED_FROM_MAAT, testBatchId, null, HttpStatus.OK, "Fetched 12 fdc entries");

		verify(eventService).logFdc(EventType.SENT_TO_DRC, testBatchId, actualFdc1, HttpStatus.OK, null);
		verify(eventService).logFdc(EventType.SENT_TO_DRC, testBatchId, actualFdc2, HttpStatus.OK, null);
		verify(eventService, times(12)).logFdc(eq(EventType.SENT_TO_DRC), eq(testBatchId), any(), eq(HttpStatus.OK), eq(null));

		verify(eventService).logFdc(EventType.FDC_GLOBAL_UPDATE, testBatchId, null, HttpStatus.OK, "Updated 3 fdc entries");

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
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(), any(), any(), any())).thenReturn("<xml>ValidAckXML</xml>");
		doNothing().when(drcClient).sendFdcReqToDrc(any());
		// run
		boolean successful = fdcService.processDailyFiles();
		// test
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils).generateFileName(any());
		verify(fdcMapperUtils).generateAckXML(any(), any(), any(), any());
		verify(fdcMapperUtils, times(12)).mapFdcEntry(any());
		verify(drcClient, times(0)).sendConcorContributionReqToDrc(any()); // nothing sent to DRC
		softly.assertThat(successful).isTrue();
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(0, postRequestedFor(urlEqualTo(PREPARE_URL))); // no FDC global update
		WireMock.verify(0, postRequestedFor(urlEqualTo(UPDATE_URL))); // no changes to statuses or contribution_files
	}

	@Test
	void testFdcGlobalUpdateError() {
		// setup
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		doNothing().when(drcClient).sendFdcReqToDrc(any());
		when(eventService.generateBatchId()).thenReturn(testBatchId);
		customStubs.add(stubFor(post(PREPARE_URL).atPriority(1)
				.willReturn(serverError())));

		Fdc expectedFdc1 = createExpectedFdc(1000, 10000000, "2050-07-12", "2011-12-03", "3805.69","3805.69", "0");
		Fdc expectedFdc2 = createExpectedFdc(4000, 40000000, "2000-07-12", "2014-03-20", "2283.1","2283.1", "0");


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
		verify(eventService).logFdc(EventType.FETCHED_FROM_MAAT, testBatchId, null, HttpStatus.OK, "Fetched 12 fdc entries");


		verify(eventService).logFdc(EventType.FDC_GLOBAL_UPDATE, testBatchId, null, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to complete FDC global update [500 Received error 500 INTERNAL_SERVER_ERROR due to Internal Server Error]");

		verify(eventService).logFdc(EventType.SENT_TO_DRC, testBatchId, actualFdc1, HttpStatus.OK, null);
		verify(eventService).logFdc(EventType.SENT_TO_DRC, testBatchId, actualFdc2, HttpStatus.OK, null);
		verify(eventService, times(12)).logFdc(eq(EventType.SENT_TO_DRC), eq(testBatchId), any(), eq(HttpStatus.OK), eq(null));

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
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		doNothing().when(drcClient).sendFdcReqToDrc(any());
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
	void testGetFdcError() {
		// setup
		customStubs.add(stubFor(get(GET_URL).atPriority(1)
				.willReturn(serverError())));
		// do
		softly.assertThatThrownBy(() -> fdcService.processDailyFiles())
				.isInstanceOf(HttpServerErrorException.class);
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
	void testAtomicUpdateFailure(){
		// setup
		customStubs.add(stubFor(post(UPDATE_URL).atPriority(1)
				.willReturn(serverError())));

		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		doNothing().when(drcClient).sendFdcReqToDrc(any());

		// do
		softly.assertThatThrownBy(() -> fdcService.processDailyFiles())
				.isInstanceOf(HttpServerErrorException.class);
		// test
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testProcessFdcUpdateWhenSuccessful() {
		UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
				.fdcId(911)
				.build();
		Integer response = fdcService.processFdcUpdate(dataRequest);
		softly.assertThat(response).isEqualTo(1111);
	}

	@Test
	void testProcessFdcUpdateWhenIncomingIsolated() {
		when(feature.incomingIsolated()).thenReturn(true);
		UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
				.fdcId(911)
				.build();
		Integer response = fdcService.processFdcUpdate(dataRequest);
		softly.assertThat(response).isEqualTo(0); // so MAAT DB not touched
	}

	@Test
	void testProcessFdcUpdateWhenFailed() {
		String errorText = "The request has failed to process";
		UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
				.fdcId(9)
				.errorText(errorText)
				.build();
		var exception = catchThrowableOfType(() -> fdcService.processFdcUpdate(dataRequest), MaatApiClientException.class);
		softly.assertThat(exception).isNotNull();
		softly.assertThat(exception.getStatusCode().is4xxClientError()).isTrue();
	}

	@SneakyThrows
	Fdc createExpectedFdc(Integer id, Integer maatId, String sentenceDate, String calculationDate, String finalCost, String lgfsTotal, String agfsTotal){
		Fdc fdc = new Fdc();
		fdc.setId(BigInteger.valueOf(id));
		fdc.setMaatId(BigInteger.valueOf(maatId));
		fdc.setSentenceDate(convertToXMLGregorianCalendar(sentenceDate));
		fdc.setCalculationDate(convertToXMLGregorianCalendar(calculationDate));
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

	private Fdc getFdcFromCaptorByFdcId(BigInteger fdcId){
		Optional<Fdc> optionalFdc =  fdcArgumentCaptor.getAllValues().stream().filter(x-> x.getId().equals(fdcId)).findFirst();
		softly.assertThat(optionalFdc.isPresent()).isTrue();
		return optionalFdc.get();
	}

}
