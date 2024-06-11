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
import static org.mockito.Mockito.when;


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

	@SpyBean
	private DrcStubRestController drcStubRestControllerSpy;

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

	@Test
	void testPositiveActiveContributionProcessing() {
		// Set 3 concor_contributions rows to status ACTIVE
		var request = UpdateConcorContributionStatusRequest.builder().status(ConcorContributionStatus.ACTIVE).recordCount(3).build();
		var idList = testDataClient.updateConcurContributionStatus(request); // REST call
		softly.assertThat(idList).hasSize(3);

		// After processDailyFiles() is called, contribSet will contain the set of contribution IDs returned by the call to getContributions.
		final var contribSet = new TreeSet<Integer>();
		when(contributionClientSpy.getContributions("ACTIVE")).thenAnswer(invocation -> {
			var entries = (List<ConcurContribEntry>) invocation.callRealMethod();
			var activeSet = entries.stream().map(ConcurContribEntry::getConcorContributionId).collect(Collectors.toSet());
			contribSet.addAll(activeSet);
			// could change this to only return three entries?
			return entries;
		});

		// After processDailyFiles() is called, these sets will contain the set of contribution IDs sent to the DRC.
		final var sentSet = new TreeSet<Integer>();
		final var successfullySentSet = new TreeSet<Integer>();
		when(drcClientSpy.sendContributionUpdate(any())).thenAnswer(invocation -> {
			var data = (SendContributionFileDataToDrcRequest) invocation.getArgument(0);
			sentSet.add(data.getContributionId());
			var result = (Boolean) invocation.callRealMethod();
			if (Boolean.TRUE.equals(result)) { // note because of the next block, this is likely always true.
				successfullySentSet.add(data.getContributionId());
			}
			return result;
		});

		// After processDailyFiles() is called, these sets will contain the set of contribution IDs received by the DRC.
		final var recvSet = new TreeSet<Integer>();
		when(drcStubRestControllerSpy.contribution(any())).thenAnswer(invocation -> {
			var data = (SendContributionFileDataToDrcRequest) invocation.getArgument(0);
			recvSet.add(data.getContributionId());
			var result = (Boolean) invocation.callRealMethod(); // always return true for our "blessed" rows.
			return idList.contains(data.getContributionId()) ? Boolean.TRUE : result;
		});

		// After processDailyFiles() is called, file will contain valid information.
        final var file = new Object() {
			Set<Integer> successfullySentSet;
			int successfullySentCount;
			String name;
			Boolean result;
        };
		when(contributionClientSpy.updateContributions(any())).thenAnswer(invocation -> {
			var data = (ContributionUpdateRequest) invocation.getArgument(0);
			file.successfullySentSet = data.getConcorContributionIds().stream().map(Integer::valueOf).collect(Collectors.toSet());
			file.successfullySentCount = data.getRecordsSent();
			file.name = data.getXmlFileName();
			file.result = (Boolean) invocation.callRealMethod();
			return file.result;
		});

		// Run processDailyFiles
		contributionService.processDailyFiles();

		// Check 1: were our 3 updated rows returned by getContributions?
		softly.assertThatCollection(contribSet).containsAll(idList);

		// Check 2: were our 3 updated rows sent to the DRC, and was there a synchronous positive response?
		softly.assertThatCollection(sentSet).containsAll(idList);
		softly.assertThatCollection(successfullySentSet).containsAll(idList);
		softly.assertThatCollection(recvSet).containsAll(idList);

		// Check 3: were there three successful contributions recorded, did they include our ids, is the name and result valid?
		softly.assertThat(file.successfullySentCount).isGreaterThanOrEqualTo(3);
		softly.assertThatCollection(file.successfullySentSet).containsAll(idList);
		softly.assertThat(file.name).isNotBlank();
		softly.assertThat(file.result).isEqualTo(Boolean.TRUE);

		// Check 4: have our 3 updated rows been reset to status SENT
		for (var id: idList) {
			ConcorContributionResponseDTO contrib = testDataClient.getConcorContribution(id); // REST call
			softly.assertThat(contrib.getStatus()).isEqualTo(ConcorContributionStatus.SENT);
		}

		// Check 5: has a contribution_files row been created>
		//TODO
	}

}
