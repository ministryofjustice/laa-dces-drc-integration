package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.ErrorResponseException;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;

import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.*;

@ExtendWith(SoftAssertionsExtension.class)
@WireMockTest(httpPort = 1111)
class ContributionAckServiceTest extends ApplicationTestBase {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@MockitoBean
	private FeatureProperties feature;

	@Autowired
	private ContributionAckService contributionAckService;

	@MockitoBean
	private EventService eventService;

    @AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testProcessContributionUpdateWhenSuccessful() {
		ConcorContributionAckFromDrc ackFromDrc = buildContribAck(CONTRIB_ID_FOUND_IN_MAAT);
		Long response = contributionAckService.handleContributionProcessedAck(ackFromDrc);
		softly.assertThat(response).isEqualTo(1111L);
		verify(eventService).logConcor(911L,EventType.DRC_ASYNC_RESPONSE,null,null, OK, STATUS_MSG_SUCCESS);
		verify(eventService).logConcorContributionError(ackFromDrc);
	}

	@Test
	void testProcessContributionUpdateWhenIncomingIsolated() {
		when(feature.incomingIsolated()).thenReturn(true);
		ConcorContributionAckFromDrc ackFromDrc = buildContribAck(CONTRIB_ID_FOUND_IN_MAAT);
		Long response = contributionAckService.handleContributionProcessedAck(ackFromDrc);
		softly.assertThat(response).isEqualTo(0L); // so MAAT DB not touched
		verify(eventService).logConcorContributionError(ackFromDrc);
	}

	@Test
	void testProcessContributionUpdateWhenNotFound() {
		String errorText = "The request has failed to process";
		ConcorContributionAckFromDrc ackFromDrc = buildContribAck(CONTRIB_ID_NOT_FOUND_IN_MAAT, errorText);
		var exception = catchThrowableOfType(ErrorResponseException.class, () -> contributionAckService.handleContributionProcessedAck(ackFromDrc));
		softly.assertThat(exception).isNotNull();
		softly.assertThat(NOT_FOUND.isSameCodeAs(exception.getStatusCode())).isTrue();
		verify(eventService).logConcor(CONTRIB_ID_NOT_FOUND_IN_MAAT, EventType.DRC_ASYNC_RESPONSE,null,null, NOT_FOUND, errorText);
		verify(eventService).logConcorContributionError(ackFromDrc);
	}

	@Test
	void testProcessContributionUpdateWhenNoContFile() {
		String errorText = "The request has failed to process";
		ConcorContributionAckFromDrc ackFromDrc = buildContribAck(CONTRIB_ID_FILE_NOT_FOUND_IN_MAAT, errorText);
		var exception = catchThrowableOfType(ErrorResponseException.class, () -> contributionAckService.handleContributionProcessedAck(ackFromDrc));
		softly.assertThat(exception).isNotNull();
		softly.assertThat(FAILED_DEPENDENCY.isSameCodeAs(exception.getStatusCode())).isTrue();
		verify(eventService).logConcor(9L,EventType.DRC_ASYNC_RESPONSE,null,null, BAD_REQUEST, errorText);
		verify(eventService).logConcorContributionError(ackFromDrc);
	}

}
