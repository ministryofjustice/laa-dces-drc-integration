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
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestConfig;
import uk.gov.justice.laa.crime.dces.integration.config.Feature;
import uk.gov.justice.laa.crime.dces.integration.datasource.CaseSubmissionService;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SoftAssertionsExtension.class)
@WireMockTest(httpPort = 1111)
class FdcServiceTest extends ApplicationTestConfig {
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
	private CaseSubmissionService caseSubmissionService;

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
}
