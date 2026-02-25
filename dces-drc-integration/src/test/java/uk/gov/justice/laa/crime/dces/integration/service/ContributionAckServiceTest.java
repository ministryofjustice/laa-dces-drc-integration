package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Instant;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FAILED_DEPENDENCY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.CONTRIB_ID_FILE_NOT_FOUND_IN_MAAT;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.CONTRIB_ID_FOUND_IN_MAAT;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.CONTRIB_ID_NOT_FOUND_IN_MAAT;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.MAAT_ID;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.buildContribAck;

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
		verify(eventService).logConcor(911L, EventType.DRC_ASYNC_RESPONSE, MAAT_ID, OK, null);
		verify(eventService).logConcorContributionAckResult(ackFromDrc, OK);
	}

	@Test
	void testProcessContributionUpdateWhenIncomingIsolated() {
		when(feature.incomingIsolated()).thenReturn(true);
		ConcorContributionAckFromDrc ackFromDrc = buildContribAck(CONTRIB_ID_FOUND_IN_MAAT);
		Long response = contributionAckService.handleContributionProcessedAck(ackFromDrc);
		softly.assertThat(response).isEqualTo(0L); // so MAAT DB not touched
		verify(eventService).logConcor(911L, EventType.DRC_ASYNC_RESPONSE, MAAT_ID, OK, null);
		verify(eventService).logConcorContributionAckResult(ackFromDrc, OK);
	}

	@Test
	void testProcessContributionUpdateWhenNotFound() {
		String errorText = "The request has failed to process";
		ConcorContributionAckFromDrc ackFromDrc = buildContribAck(CONTRIB_ID_NOT_FOUND_IN_MAAT, errorText);
		var exception = catchThrowableOfType(ErrorResponseException.class, () -> contributionAckService.handleContributionProcessedAck(ackFromDrc));
    softly.assertThat(exception.getStatusCode()).isEqualTo(NOT_FOUND);
    softly.assertThat(exception.getBody().getTitle()).isEqualTo("Contribution ID not found");
    softly.assertThat(exception.getBody().getDetail()).isEqualTo("Contribution ID 404 not found");
		verify(eventService).logConcor(CONTRIB_ID_NOT_FOUND_IN_MAAT, EventType.DRC_ASYNC_RESPONSE, MAAT_ID, NOT_FOUND, errorText);
		verify(eventService).logConcorContributionAckResult(ackFromDrc, NOT_FOUND);
	}

	@Test
	void testProcessContributionUpdateWhenNoContFile() {
		String errorText = "The request has failed to process";
		ConcorContributionAckFromDrc ackFromDrc = buildContribAck(CONTRIB_ID_FILE_NOT_FOUND_IN_MAAT, errorText);
		var exception = catchThrowableOfType(ErrorResponseException.class, () -> contributionAckService.handleContributionProcessedAck(ackFromDrc));
    softly.assertThat(exception.getStatusCode()).isEqualTo(FAILED_DEPENDENCY);
    softly.assertThat(exception.getBody().getTitle()).isEqualTo("Corresponding Contribution File not found");
    softly.assertThat(exception.getBody().getDetail()).isEqualTo("Contribution ID 9 is not associated with a Contribution File");
		verify(eventService).logConcor(9L, EventType.DRC_ASYNC_RESPONSE, MAAT_ID, BAD_REQUEST, errorText);
		verify(eventService).logConcorContributionAckResult(ackFromDrc, FAILED_DEPENDENCY);
	}

	@Test
	void testDuplicateRequestIsDetected() {
		// given
		ConcorContributionAckFromDrc ackFromDrc = buildContribAck(CONTRIB_ID_FOUND_IN_MAAT, "Success");
		// expect
		when(eventService.concorContributionAlreadyProcessed(
				Mockito.eq(ackFromDrc.data().concorContributionId()),
				Mockito.any(Instant.class))).thenReturn(true);
		// when
		var exception = catchThrowableOfType(ErrorResponseException.class, () -> contributionAckService.handleContributionProcessedAck(ackFromDrc));
		// then
		softly.assertThat(exception.getStatusCode()).isEqualTo(CONFLICT);
		softly.assertThat(exception.getBody().getType().toString()).isEqualTo("https://laa-debt-collection.service.justice.gov.uk/problem-types#duplicate-request");
		softly.assertThat(exception.getBody().getTitle()).isEqualTo("Contribution acknowledgment already processed");
	}
}
