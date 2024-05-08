package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
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
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;


import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIf(expression = "#{environment['sentry.environment'] == 'DEV'}", loadContext = true)
@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest(httpPort = 1111)
class FdcIntegrationTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@InjectMocks
	@Autowired
	private FdcService fdcService;

	@MockBean
	FdcMapperUtils fdcMapperUtils;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}


	@Test
	void testProcessFdcUpdateWhenReturnedTrue() {
		UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
				.fdcId(31774046)
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

}