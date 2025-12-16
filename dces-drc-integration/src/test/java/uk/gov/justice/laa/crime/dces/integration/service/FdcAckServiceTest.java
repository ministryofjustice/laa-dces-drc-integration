package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.ErrorResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FAILED_DEPENDENCY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.*;

@ExtendWith(SoftAssertionsExtension.class)
@WireMockTest(httpPort = 1111)
class FdcAckServiceTest extends ApplicationTestBase {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	@InjectMocks
	private FdcAckService fdcAckService;

	@MockitoBean
	FdcMapperUtils fdcMapperUtils;

	@MockitoBean
	private DrcClient drcClient;

	@MockitoBean
	private FeatureProperties feature;

	@MockitoBean
	private EventService eventService;

	@AfterEach
	void afterTestAssertAll() {
		softly.assertAll();
	}

	@Test
	void testProcessFdcUpdateWhenSuccessful() {
        FdcAckFromDrc fdcAckFromDrc = buildFdcAck(FDC_ID_FOUND_IN_MAAT);
		Long response = fdcAckService.handleFdcProcessedAck(fdcAckFromDrc);
		softly.assertThat(response).isEqualTo(1111L);
		verify(eventService).logFdcError(fdcAckFromDrc);
	}

	@Test
	void testProcessFdcUpdateWhenIncomingIsolated() {
		when(feature.incomingIsolated()).thenReturn(true);
		FdcAckFromDrc fdcAckFromDrc = buildFdcAck(FDC_ID_FOUND_IN_MAAT);
		Long response = fdcAckService.handleFdcProcessedAck(fdcAckFromDrc);
		softly.assertThat(response).isEqualTo(0L); // so MAAT DB not touched
		verify(eventService).logFdcError(fdcAckFromDrc);
	}

	@Test
	void testProcessFdcUpdateWhenNotFound() {
		String errorText = "The request has failed to process";
		FdcAckFromDrc fdcAckFromDrc = buildFdcAck(FDC_ID_NOT_FOUND_IN_MAAT, errorText);
		var exception = catchThrowableOfType(ErrorResponseException.class, () -> fdcAckService.handleFdcProcessedAck(fdcAckFromDrc));
		softly.assertThat(exception).isNotNull();
		softly.assertThat(NOT_FOUND.isSameCodeAs(exception.getStatusCode())).isTrue();
		verify(eventService).logFdcError(fdcAckFromDrc);
	}

	@Test
	void testProcessFdcUpdateWhenNoContFile() {
		String errorText = "The request has failed to process";
		FdcAckFromDrc fdcAckFromDrc = buildFdcAck(FDC_ID_FILE_NOT_FOUND_IN_MAAT, errorText);
		var exception = catchThrowableOfType(ErrorResponseException.class, () -> fdcAckService.handleFdcProcessedAck(fdcAckFromDrc));
		softly.assertThat(exception).isNotNull();
		softly.assertThat(FAILED_DEPENDENCY.isSameCodeAs(exception.getStatusCode())).isTrue();
		verify(eventService).logFdcError(fdcAckFromDrc);
	}

	@Test
	void testProcessFdcUpdateWhenServerFailure() {
		String errorText = "The request has failed to process";
		FdcAckFromDrc fdcAckFromDrc = buildFdcAck(500L, errorText);
		var exception = catchThrowableOfType(ErrorResponseException.class, () -> fdcAckService.handleFdcProcessedAck(fdcAckFromDrc));
		softly.assertThat(exception).isNotNull();
		softly.assertThat(exception.getStatusCode().is5xxServerError()).isTrue();
		verify(eventService).logFdcError(fdcAckFromDrc);
	}

}
