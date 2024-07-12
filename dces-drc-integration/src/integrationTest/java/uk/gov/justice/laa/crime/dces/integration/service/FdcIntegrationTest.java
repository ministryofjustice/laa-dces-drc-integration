package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.LocalDate;
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
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;
import uk.gov.justice.laa.crime.dces.integration.testing.FdcProcessSpy;
import uk.gov.justice.laa.crime.dces.integration.testing.SpyFactory;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;


import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest(httpPort = 1111)
class FdcIntegrationTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	private SpyFactory spyFactory;

	@Autowired
	private FdcService fdcService;

	@Autowired
	private TestDataClient testDataClient;

	@Autowired
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
		softly.assertThat(response).isEqualTo("The request has been processed successfully");
	}

	@Test
	void testProcessFdcUpdateWhenReturnedFalse() {
		String errorText = "The request has failed to process";
		UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
				.fdcId(9)
				.errorText(errorText)
				.build();
		String response = fdcService.processFdcUpdate(dataRequest);
		softly.assertThat(response).isEqualTo("The request has failed to process");
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A positive FDC Contributions integration test which checks that "Delayed Pickup" logic correctly identifies its
	 * corresponding set of WAITING_ITEMS records, to convert to a REQUESTED status
	 *     check that REQUESTED records get picked up and processed correctly</p>
	 * <h4>Given:</h4>
	 * <p>* Insert FDC_CONTRIBUTIONS records with the WAITING_ITEMS status and FDC_ITEMS for 3 rep IDs identified
	 * for the purposes of the test.</p>
	 * <h4>When:</h4>
	 * <p>* The {@link FdcService#processDailyFiles()} method is called.</p>
	 * <h4>Then:</h4>
	 * <p>1. The call to the callGlobalUpdate is successful i.e. MAAT API returned a successful response
	 * <p>2. The IDs of the 3 updated records are returned.</p>
	 * <p>3. The updated IDs are included in the list of IDs returned by the call to retrieve 'REQUESTED' FDC Contributions.</p>
	 * <p>4. The updated IDs are included in the set of payloads sent to the DRC.</p>
	 * <p>5. Synchronous responses are received from the DRC for these IDs
	 * <p>6. A contribution file gets created, with at least 3 successful records and 0 failed records.<br>
	 *    The generated XML content includes each of the 3 linked rep_order IDs in a &lt;maat_id&gt; element tag.</p>
	 * <p>6. After the `processDailyFiles` method call returns, the fdc_contribution entities corresponding to each
	 *    of the updated IDs is checked:<br>
	 *    - Each now has status SENT<br>
	 *    - Each now has a valid contribution_file ID<br>
	 *    - Each now has a last modified user of "DCES"<br>
	 *    - Each now has a last modified time during the method call.</p>
	 * <p>7. After the `processDailyFiles` method call returns, the contribution_file entity that was created is
	 *    checked:<br>
	 *    - It has the expected filename<br>
	 *    - It has a "records sent" of greater than or equal to 3<br>
	 *    - It has a created and last modified user of "DCES"<br>
	 *    - It has a created, sent and last modified time during the method call<br>
	 *    - The fetched XML content includes each of the 3 linked rep_order IDs in a &lt;maat_id&gt; element tag.</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-356</a> for test specification.
	 */
	@Test
	void givenSomeWaitingItemsFdcContributions_whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile() {
		// Set up test data for the scenario:
		final var updatedIds = spyFactory.createFdcDelayedPickupTestData(FdcTestType.POSITIVE, 3);

		final FdcProcessSpy.FdcProcessSpyBuilder watching = spyFactory.newFdcProcessSpyBuilder()
				.traceExecuteFdcGlobalUpdate()
				.traceAndFilterGetFdcContributions(updatedIds)
				.traceAndStubSendFdcUpdate(id -> Boolean.TRUE)
				.traceUpdateFdcs();

		// Call the processDailyFiles() method under test:
		final var startDate = LocalDate.now();
		fdcService.processDailyFiles();
		final var endDate = LocalDate.now();

		final FdcProcessSpy watched = watching.build();

		// Fetch some items of information from the maat-api to use during validation:
		final var fdcContributions = updatedIds.stream().map(testDataClient::getFdcContribution).toList();
		final int contributionFileId = watched.getXmlFileResult();
		final var contributionFile = testDataClient.getContributionFile(contributionFileId);

		softly.assertThat(watched.getGlobalUpdateResponse().isSuccessful()).isTrue(); // 1
		softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 2.
		softly.assertThat(watched.getActiveIds()).containsAll(updatedIds); // 3.
		softly.assertThat(watched.getSentIds()).containsAll(updatedIds); // 4.

		softly.assertThat(watched.getRecordsSent()).isGreaterThanOrEqualTo(3); // 5.
		softly.assertThat(watched.getXmlCcIds()).containsAll(updatedIds);
		fdcContributions.forEach(fdcContribution ->
				softly.assertThat(watched.getXmlContent()).contains("<maat_id>" + fdcContribution.getMaatId() + "</maat_id>"));

		fdcContributions.forEach(fdcContribution -> { // 6.
			softly.assertThat(fdcContribution.getStatus()).isEqualTo(ConcorContributionStatus.SENT);
			softly.assertThat(fdcContribution.getContFileId()).isEqualTo(contributionFileId);
			softly.assertThat(fdcContribution.getUserModified()).isEqualTo("DCES");
			softly.assertThat(fdcContribution.getDateModified()).isBetween(startDate, endDate);
		});

		softly.assertThat(contributionFile.getXmlFileName()).isEqualTo(watched.getXmlFileName()); //6.
		softly.assertThat(contributionFile.getId()).isEqualTo(watched.getXmlFileResult());
		softly.assertThat(contributionFile.getRecordsSent()).isGreaterThanOrEqualTo(3);
		softly.assertThat(contributionFile.getDateCreated()).isBetween(startDate, endDate);
		softly.assertThat(contributionFile.getUserCreated()).isEqualTo("DCES");
		// TODO uncomment after fix null actual: softly.assertThat(contributionFile.getDateModified()).isBetween(startDate, endDate);
		// TODO uncomment after fix null actual: softly.assertThat(contributionFile.getUserModified()).isEqualTo("DCES");
		softly.assertThat(contributionFile.getDateSent()).isBetween(startDate, endDate);
		fdcContributions.forEach(fdcContribution ->
				softly.assertThat(contributionFile.getXmlContent()).contains("<maat_id>" + fdcContribution.getMaatId() + "</maat_id>"));
	}

}