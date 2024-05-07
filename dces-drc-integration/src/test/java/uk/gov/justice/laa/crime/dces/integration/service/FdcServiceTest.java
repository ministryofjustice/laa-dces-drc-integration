package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.ObjectFactory;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest(httpPort = 1111)
class FdcServiceTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@InjectMocks
	@Autowired
	private FdcService fdcService;

	@MockBean
	FdcMapperUtils fdcMapperUtils;

	@MockBean
	private DrcClient drcClient;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
		for(StubMapping stub: customStubs ){
			WireMock.removeStub(stub);
		}
	}

	private static final List<StubMapping> customStubs = new ArrayList<>();
	private static final String GET_URL = "/debt-collection-enforcement/fdc-contribution-files?status=REQUESTED";
	private static final String PREPARE_URL = "/debt-collection-enforcement/prepare-fdc-contributions-files";
	private static final String UPDATE_URL = "/debt-collection-enforcement/create-fdc-file";

	@Test
	void testXMLValid() {
		// setup
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcUpdate(any())).thenReturn(true);
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
	void testFdcGlobalUpdateError(){
		// setup
		ObjectFactory of = new ObjectFactory();
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("<xml>ValidXML</xml>");
		when(fdcMapperUtils.mapFdcEntry(any())).thenCallRealMethod();
		when(fdcMapperUtils.generateFileName(any())).thenReturn("Test.xml");
		when(fdcMapperUtils.generateAckXML(any(),any(),any(),any())).thenReturn("<xml>ValidAckXML</xml>");
		when(drcClient.sendFdcUpdate(any())).thenReturn(true);
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
		Exception exception = assertThrows(HttpServerErrorException.class, () -> {
			fdcService.processDailyFiles();
		});
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
		when(drcClient.sendFdcUpdate(any())).thenReturn(true);

		// do
		Exception exception = assertThrows(HttpServerErrorException.class, () -> {
			fdcService.processDailyFiles();
		});
		// test
		WireMock.verify(1, postRequestedFor(urlEqualTo(PREPARE_URL)));
		WireMock.verify(1, getRequestedFor(urlEqualTo(GET_URL)));
		WireMock.verify(1, postRequestedFor(urlEqualTo(UPDATE_URL)));
	}

	@Test
	void testProcessFdcUpdateWhenReturnedTrue() {
		UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
				.fdcId(911)
				.build();
		String response = fdcService.processFdcUpdate(dataRequest);
		assertEquals("The request has been processed successfully", response);
	}

	@Test
	void testProcessFdcUpdateWhenReturnedFalse() {
		String errorText = "The request has failed to process";
		UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
				.fdcId(9)
				.errorText(errorText)
				.build();
		String response = fdcService.processFdcUpdate(dataRequest);
		assertEquals("The request has failed to process", response);
	}

	private Fdc generateFdc(){
		ObjectFactory of = new ObjectFactory();
		Fdc fdc = of.createFdcFileFdcListFdc();
		fdc.setId(BigInteger.ONE);
		return fdc;
	}
}