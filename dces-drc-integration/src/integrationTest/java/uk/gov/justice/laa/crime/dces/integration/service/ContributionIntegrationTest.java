package uk.gov.justice.laa.crime.dces.integration.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.controller.DrcStubRestController;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.SendContributionFileDataToDrcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionResponseDTO;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;


@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContributionIntegrationTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	private ContributionService contributionService;

	@Autowired
	private TestDataClient testDataClient;

	@SpyBean
	private ContributionClient contributionClientSpy;

	@SpyBean
	private DrcClient drcClientSpy;

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

	@Builder
	@Getter
	static class ContributionProcessing {
		@Singular private Set<Integer> contribs; // returned by ContributionClient.getContributions("ACTIVE")
		@Singular private Set<Integer> sents; // sent to the DRC using DrcClient.
		@Singular private Set<Integer> successfullySents; // sent to ContributionClient.updateContribution()
		private int successfullySentCount; // sent to ContributionClient.updateContribution()
		private String name; // sent to ContributionClient.updateContribution()
		private Boolean result; // returned by ContributionClient.updateContribution()
	}

	@Test
	void testPositiveActiveContributionProcessing() {
		// Set three rows in concor_contributions to status `ACTIVE`
		var request = UpdateConcorContributionStatusRequest.builder().status(ConcorContributionStatus.ACTIVE).recordCount(3).build();
		var idList = testDataClient.updateConcurContributionStatus(request); // REST call
		softly.assertThat(idList).hasSize(3);

		final var processing = ContributionProcessing.builder();
		// processDailyFiles() calls ContributionsClient.getContributions("ACTIVE");
		doAnswer(invocation -> {
			var result = (List<ConcurContribEntry>) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
			processing.contribs(result.stream().map(ConcurContribEntry::getConcorContributionId).collect(Collectors.toSet()));
			return result; // could change this to only return three entries?
		}).when(contributionClientSpy).getContributions("ACTIVE");
		// processDailyFiles() calls DrcClient.sendContributionUpdate(...);
		doAnswer(invocation -> {
			var data = (SendContributionFileDataToDrcRequest) invocation.getArgument(0);
			processing.sent(data.getContributionId());
			return Boolean.TRUE;
		}).when(drcClientSpy).sendContributionUpdate(any());
		// processDailyFiles() calls ContributionClient.updateContributions(...);
		doAnswer(invocation -> {
			var data = (ContributionUpdateRequest) invocation.getArgument(0);
			processing.successfullySents(data.getConcorContributionIds().stream().map(Integer::valueOf).collect(Collectors.toSet()));
			processing.successfullySentCount(data.getRecordsSent());
			processing.name(data.getXmlFileName());
			processing.result((Boolean) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation));
			return processing.result;
		}).when(contributionClientSpy).updateContributions(any());

		// Actually execute processDailyFiles() now:
		contributionService.processDailyFiles();

		ContributionProcessing process = processing.build();

		// Check 1: were our updated rows returned by getContributions?
		softly.assertThatCollection(process.getContribs()).containsAll(idList);

		// Check 2: were our updated rows sent to the pseudo DRC?
		softly.assertThatCollection(process.getSents()).containsAll(idList);

		// Check 3: were successful contributions recorded for our rows, and is the name and result valid?
		softly.assertThat(process.getSuccessfullySentCount()).isGreaterThanOrEqualTo(3);
		softly.assertThatCollection(process.getSuccessfullySents()).containsAll(idList);
		softly.assertThat(process.getName()).isNotBlank();
		softly.assertThat(process.getResult()).isEqualTo(Boolean.TRUE);

		// Check 4: have our updated rows been reset to status SENT
		for (var id: idList) {
			ConcorContributionResponseDTO contrib = testDataClient.getConcorContribution(id); // REST call
			softly.assertThat(contrib.getStatus()).isEqualTo(ConcorContributionStatus.SENT);
		}

		// Check 5: has a contribution_files row been created>
		//TODO
	}

}
