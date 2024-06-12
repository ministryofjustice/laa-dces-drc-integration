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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.SendContributionFileDataToDrcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionResponseDTO;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;


@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@SpringBootTest
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
	static class ContributionProcess {
		@Singular private List<Integer> updatedIds; // returned from maat-api by TestDataClient.updateConcurContributionStatus(...)
		@Singular private Set<Integer> activeIds;   // returned from maat-api by ContributionClient.getContributions("ACTIVE")
		@Singular private Set<Integer> sentIds;     // sent to the DRC by DrcClient.sendContributionUpdate(...)
		@Singular private Set<Integer> fileIds;     // sent to maat-api by ContributionClient.updateContribution(...)
		private int fileIdCount;                    //   "    "    "
		private String fileName;                    //   "    "    "
		private Boolean fileResult;                 // returned from maat-api by ContributionClient.updateContribution(...)
	}

	@Test
	void testPositiveActiveContributionProcessing() {
		final var processing = ContributionProcess.builder();

		var request = UpdateConcorContributionStatusRequest.builder().status(ConcorContributionStatus.ACTIVE).recordCount(3).build();
		processing.updatedIds(testDataClient.updateConcurContributionStatus(request)); // set three rows in concor_contributions to ACTIVE
		
		doAnswer(invocation -> {
			// Because ContributionClient is a proxied interface, cannot just call `invocation.callRealMethod()` here - see https://github.com/spring-projects/spring-boot/issues/36653
			var result = (List<ConcurContribEntry>) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
			processing.activeIds(result.stream().map(ConcurContribEntry::getConcorContributionId).collect(Collectors.toSet()));
			return result;
		}).when(contributionClientSpy).getContributions("ACTIVE");

		doAnswer(invocation -> {
			var data = (SendContributionFileDataToDrcRequest) invocation.getArgument(0);
			processing.sentId(data.getContributionId());
			return Boolean.TRUE;
		}).when(drcClientSpy).sendContributionUpdate(any());

		doAnswer(invocation -> {
			var data = (ContributionUpdateRequest) invocation.getArgument(0);
			processing.fileIds(data.getConcorContributionIds().stream().map(Integer::valueOf).collect(Collectors.toSet()));
			processing.fileIdCount(data.getRecordsSent());
			processing.fileName(data.getXmlFileName());
			processing.fileResult((Boolean) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation));
			return processing.fileResult;
		}).when(contributionClientSpy).updateContributions(any());

		contributionService.processDailyFiles();

		var processed = processing.build();

		softly.assertThat(processed.getUpdatedIds()).hasSize(3); // how many rows did we try to update to ACTIVE?

		softly.assertThatCollection(processed.getActiveIds()).containsAll(processed.getUpdatedIds()); // were our rows actually updated to ACTIVE?

		softly.assertThatCollection(processed.getSentIds()).containsAll(processed.getUpdatedIds()); // were our rows sent to the DRC?

		softly.assertThat(processed.getFileIdCount()).isGreaterThanOrEqualTo(3); // were enough rows processed?
		softly.assertThatCollection(processed.getFileIds()).containsAll(processed.getUpdatedIds()); // were our rows processed?
		softly.assertThat(processed.getFileName()).isNotBlank(); // was an XML filename generated?
		softly.assertThat(processed.getFileResult()).isEqualTo(Boolean.TRUE); // was contribution_files row created ok?

		processed.getUpdatedIds().forEach(id -> {
			var contribution = testDataClient.getConcorContribution(id);
			softly.assertThat(contribution.getStatus()).isEqualTo(ConcorContributionStatus.SENT); // were our rows reset to SENT?
		});

		// TODO: Check that a contribution_files row has been created with the XML filename from before
	}

}
