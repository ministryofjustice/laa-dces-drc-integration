package uk.gov.justice.laa.crime.dces.integration.service;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogContributionRequest;


@EnabledIf(expression = "#{environment['sentry.environment'] == 'DEV'}", loadContext = true)
@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContributionIntegrationTest {

	@InjectSoftAssertions
	private SoftAssertions softly;


	@Autowired
	private ContributionService contributionService;

	@AfterEach
	void assertAll(){
		softly.assertAll();
	}

	@Test
	void testProcessContributionUpdateWhenReturnedFalse() {
		String errorText = "The request has failed to process";
		UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
				.concorId(9)
				.errorText(errorText)
				.build();
		String response = contributionService.processContributionUpdate(dataRequest);
		softly.assertThat(response).isEqualTo("The request has failed to process");
	}


	@Test
	void testProcessContributionUpdateWhenReturnedTrue() {
		String errorText = "Error Text updated successfully.";
		UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
				.concorId(47959912)
				.errorText(errorText)
				.build();
		String response = contributionService.processContributionUpdate(dataRequest);
		softly.assertThat(response).isEqualTo("The request has been processed successfully");
	}

}
