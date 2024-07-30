package uk.gov.justice.laa.crime.dces.integration.service;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcAccelerationType;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;
import uk.gov.justice.laa.crime.dces.integration.testing.FdcProcessSpy;
import uk.gov.justice.laa.crime.dces.integration.testing.SpyFactory;

import java.time.LocalDate;
import java.util.Set;

@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FdcIntegrationTest {
	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	private SpyFactory spyFactory;

	@Autowired
	private FdcService fdcService;

	@Autowired
	private TestDataClient testDataClient;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testProcessFdcUpdateWhenFound() {
		final var updateLogFdcRequest = UpdateLogFdcRequest.builder()
				.fdcId(31774046)
				.build();
		final String response = fdcService.processFdcUpdate(updateLogFdcRequest);
		softly.assertThat(response).isEqualTo("The request has been processed successfully");
	}

	@Test
	void testProcessFdcUpdateWhenNotFound() {
		final String errorText = "Error Text updated successfully.";
		final var updateLogFdcRequest = UpdateLogFdcRequest.builder()
				.fdcId(9)
				.errorText(errorText)
				.build();
		final String response = fdcService.processFdcUpdate(updateLogFdcRequest);
		softly.assertThat(response).isEqualTo("The request has failed to process");
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A positive FDC Contributions integration test which checks that "Delayed Pickup" logic correctly identifies
	 *    its corresponding set of WAITING_ITEMS records, to convert to a REQUESTED status, and checks that REQUESTED
	 *    records get picked up and processed correctly</p>
	 * <h4>Given:</h4>
	 * <p>* Insert FDC_CONTRIBUTIONS records with the WAITING_ITEMS status and FDC_ITEMS for 3 rep IDs identified
	 *      for the purposes of the test.</p>
	 * <h4>When</h4>
	 * <p>* See {@link #whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(Set)}</p>
	 * <h4>Then:</h4>
	 * <p>* See {@link #whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(Set)}</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-356</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenSomeDelayedPickupWaitingItemsFdcContributions_whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile() {
		// Set up test data for the scenario:
		final var updatedIds = spyFactory.createFdcDelayedPickupTestData(FdcTestType.POSITIVE, 3);

		whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(updatedIds);
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A positive integration test to check that the "positive acceleration decode condition" logic can correctly
	 *    identify the correct set of WAITING_ITEMS fdc_contributions records, to convert to REQUESTED status, and then
	 *    check that REQUESTED records get picked up and processed correctly</p>
	 * <h4>Given:</h4>
	 * <p>* Insert fdc_contributions records with the WAITING_ITEMS status and fdc_items for 3 rep order IDs identified
	 *      for the purposes of the test.</p>
	 * <h4>When</h4>
	 * <p>* See {@link #whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(Set)}</p>
	 * <h4>Then:</h4>
	 * <p>* See {@link #whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(Set)}</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-357">DCES-357</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenSomePositiveAcceleratedWaitingItemsFdcContributions_whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile() {
		// Set up test data for the scenario:
		final var updatedIds = spyFactory.createFastTrackTestData(FdcAccelerationType.POSITIVE, FdcTestType.POSITIVE, 3);

		whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(updatedIds);
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A positive integration test to check that the "negative acceleration decode condition" logic can correctly
	 *    identify the correct set of WAITING_ITEMS fdc_contributions records, to convert to REQUESTED status, and then
	 *    check that REQUESTED records get picked up and processed correctly</p>
	 * <h4>Given:</h4>
	 * <p>* Insert fdc_contributions records with the WAITING_ITEMS status and fdc_items for 3 rep order IDs identified
	 *      for the purposes of the test.</p>
	 * <h4>When</h4>
	 * <p>* See {@link #whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(Set)}</p>
	 * <h4>Then:</h4>
	 * <p>* See {@link #whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(Set)}</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-358">DCES-358</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenSomeNegativeAcceleratedWaitingItemsFdcContributions_whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile() {
		// Set up test data for the scenario:
		final var updatedIds = spyFactory.createFastTrackTestData(FdcAccelerationType.NEGATIVE, FdcTestType.POSITIVE, 3);

		whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(updatedIds);
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A positive integration test to check that the "previous FDC fast track" logic can correctly
	 *    identify the correct set of WAITING_ITEMS fdc_contributions records, to convert to REQUESTED status, and then
	 *    check that REQUESTED records get picked up and processed correctly</p>
	 * <h4>Given:</h4>
	 * <p>* Insert fdc_contributions records with the WAITING_ITEMS status and fdc_items for 3 rep order IDs identified
	 *      for the purposes of the test.</p>
	 * <h4>When</h4>
	 * <p>* See {@link #whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(Set)}</p>
	 * <h4>Then:</h4>
	 * <p>* See {@link #whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(Set)}</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-359">DCES-359</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenSomeFastTrackPreviousFdcWaitingItemsFdcContributions_whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile() {
		// Set up test data for the scenario:
		final var updatedIds = spyFactory.createFastTrackTestData(FdcAccelerationType.PREVIOUS_FDC, FdcTestType.POSITIVE, 3);

		whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(updatedIds);
	}

	/**
	 * <p>Test utility method for the when/then processing of positive FDC contribution integration tests.</p>
	 * <h4>Given:</h4>
	 * <p>* Some set of updated fdc_contributions IDs that processDailyFiles should act upon (passed in to this method
	 *      as the {@code updatedIds} method parameter).</p>
	 * <h4>When</h4>
	 * <p>* The {@link FdcService#processDailyFiles()} method is called.</p>
	 * <h4>Then:</h4>
	 * <p>1. The call to the callGlobalUpdate is successful i.e. MAAT API returned a successful response
	 * <p>2. The IDs of the 3 updated records are returned.</p>
	 * <p>3. The updated IDs are included in the list of IDs returned by the call to retrieve 'REQUESTED' FDC Contributions.</p>
	 * <p>4. The updated IDs are included in the set of payloads sent to the DRC.</p>
	 * <p>5. Synchronous responses are received from the DRC for these IDs
	 * <p>6. A contribution file gets created, with at least 3 successful records and 0 failed records.<br>
	 *       The generated XML content includes each of the 3 linked rep_order IDs in a &lt;maat_id&gt; element tag.</p>
	 * <p>7. After the `processDailyFiles` method call returns, the fdc_contribution entities corresponding to each
	 *       of the updated IDs is checked:<br>
	 *       - Each now has status SENT<br>
	 *       - Each now has a valid contribution_file ID<br>
	 *       - Each now has a last modified user of "DCES"<br>
	 *       - Each now has a last modified time during the method call.</p>
	 * <p>8. After the `processDailyFiles` method call returns, the contribution_file entity that was created is
	 *       checked:<br>
	 *       - It has the expected filename<br>
	 *       - It has a "records sent" of greater than or equal to 3<br>
	 *       - It has a created and last modified user of "DCES"<br>
	 *       - It has a created, sent and last modified time during the method call<br>
	 *       - The fetched XML content includes each of the 3 linked rep_order IDs in a &lt;maat_id&gt; element tag.</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-356">DCES-356</a>,
	 *      <a href="https://dsdmoj.atlassian.net/browse/DCES-357">DCES-357</a>,
	 *      <a href="https://dsdmoj.atlassian.net/browse/DCES-358">DCES-358</a> and
	 *      <a href="https://dsdmoj.atlassian.net/browse/DCES-359">DCES-359</a> for test specifications.
	 */
	private void whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile(final Set<Integer> updatedIds) {
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
		softly.assertThat(watched.getRequestedIds()).containsAll(updatedIds); // 3.
		softly.assertThat(watched.getSentIds()).containsAll(updatedIds); // 4.

		softly.assertThat(watched.getRecordsSent()).isGreaterThanOrEqualTo(3); // 5.
		softly.assertThat(watched.getXmlCcIds()).containsAll(updatedIds);

		fdcContributions.forEach(fdcContribution -> { // 6.
			softly.assertThat(watched.getXmlContent()).contains("<fdc id=\""+fdcContribution.getId()+"\">");
			// TODO uncomment the following line - it should work but doesn't at the moment because maat API
			//      endpoint /debt-collection-enforcement/fdc-contribution returns the maatId as null.
			// softly.assertThat(watched.getXmlContent()).contains("<maat_id>" + fdcContribution.getMaatId() + "</maat_id>");
		});

		fdcContributions.forEach(fdcContribution -> { // 7.
			softly.assertThat(fdcContribution.getStatus()).isEqualTo(FdcContributionsStatus.SENT);
			softly.assertThat(fdcContribution.getContFileId()).isEqualTo(contributionFileId);
			softly.assertThat(fdcContribution.getUserModified()).isEqualTo("DCES");
			softly.assertThat(fdcContribution.getDateModified()).isBetween(startDate, endDate);
		});

		softly.assertThat(contributionFile.getXmlFileName()).isEqualTo(watched.getXmlFileName()); // 8.
		softly.assertThat(contributionFile.getId()).isEqualTo(watched.getXmlFileResult());
		softly.assertThat(contributionFile.getRecordsSent()).isGreaterThanOrEqualTo(3);
		softly.assertThat(contributionFile.getDateCreated()).isBetween(startDate, endDate);
		softly.assertThat(contributionFile.getUserCreated()).isEqualTo("DCES");
		// TODO uncomment after fix null actual: softly.assertThat(contributionFile.getDateModified()).isBetween(startDate, endDate);
		// TODO uncomment after fix null actual: softly.assertThat(contributionFile.getUserModified()).isEqualTo("DCES");
		softly.assertThat(contributionFile.getDateSent()).isBetween(startDate, endDate);
		fdcContributions.forEach(fdcContribution -> {
			softly.assertThat(contributionFile.getXmlContent()).contains("<fdc id=\""+fdcContribution.getId()+"\">");
			// TODO uncomment the following line - it should work but doesn't at the moment because maat API
			//      endpoint /debt-collection-enforcement/fdc-contribution returns the maatId as null.
			// softly.assertThat(contributionFile.getXmlContent()).contains("<maat_id>" + fdcContribution.getMaatId() + "</maat_id>");
		});

	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A negative integration test to check that fdc_contributions having status SENT are NOT picked up and are not
	 *    processed.</p>
	 * <h4>Given:</h4>
	 * <p>* 3 fdc_contributions record IDs that have been updated to the SENT status.</p>
	 * <h4>When</h4>
	 * <p>* The {@link FdcService#processDailyFiles()} method is called.</p>
	 * <h4>Then:</h4>
	 * <p>1. The call to the callGlobalUpdate is successful i.e. MAAT API returned a successful response
	 * <p>2. The IDs of the 3 updated records are returned.</p>
	 * <p>3. The updated IDs are NOT included in the list of IDs returned by the call to retrieve 'REQUESTED' FDC Contributions.</p>
	 * <p>4. The updated IDs are NOT included in the set of payloads sent to the DRC.</p>
	 * <p>5. A contribution file may or may not get created (if it is created the updated IDs are not sent to the MAAT API).</p>
	 * <p>6. After the `processDailyFiles` method call returns, the fdc_contribution entities corresponding to each
	 *       of the updated IDs is checked:<br>
	 *       - Each remains at status SENT<br>
	 *       - Each has an unpopulated contribution_file ID.</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-360">DCES-360</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenSomeSentFdcContributions_whenProcessDailyFilesRuns_thenTheyAreNotQueriedNotSentNorInCreatedFile() {
		final var updatedIds = spyFactory.createFdcDelayedPickupTestData(FdcTestType.NEGATIVE_FDC_STATUS, 3);
		runProcessDailyFilesAndCheckResults(updatedIds, true, false, false, true, FdcContributionsStatus.SENT);
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A negative integration test to check that fdc_contributions having status REQUESTED are NOT updated to SENT if
	 *    DRC processing responded synchronously with failure.</p>
	 * <h4>Given:</h4>
	 * <p>* 3 fdc_contributions record IDs that have been updated to the REQUESTED status.</p>
	 * <h4>When</h4>
	 * <p>* The {@link FdcService#processDailyFiles()} method is called (but the DRC responds negatively in its
	 *      synchronous responses).</p>
	 * <h4>Then:</h4>
	 * <p>1. The call to the callGlobalUpdate is successful i.e. MAAT API returned a successful response
	 * <p>2. The IDs of the 3 updated records are returned.</p>
	 * <p>3. The updated IDs are included in the list of IDs returned by the call to retrieve 'REQUESTED' FDC Contributions.</p>
	 * <p>4. The updated IDs are included in the set of payloads sent to the DRC.</p>
	 * <p>5. A contribution file may or may not get created (if it is created the updated IDs are not sent to the MAAT API).</p>
	 * <p>6. After the `processDailyFiles` method call returns, the fdc_contribution entities corresponding to each
	 *       of the updated IDs is checked:<br>
	 *       - Each remains at status REQUESTED.</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-360">DCES-361</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenRequestedFdcContributions_whenProcessDailyFilesFailsToSend_thenTheirStatusIsNotUpdated() {
		// Set up test data for the scenario:
		final var updatedIds = spyFactory.createFdcDelayedPickupTestData(FdcTestType.POSITIVE, 3);
		runProcessDailyFilesAndCheckResults(updatedIds, false, true, true, true, FdcContributionsStatus.REQUESTED);
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A negative FDC Contributions test which checks that FDC Contribution records do not get picked up for processing by the Delayed pickup logic,
	 * 	if there are no Rep Order Crown Court Outcomes linked to the Rep Orders on which the FDC Contribution records are based.</p>
	 * <h4>Given:</h4>
	 * <p>* 3 fdc_contributions record IDs that would normally get picked up by the Delayed pickup logic,
	 * 		but their Rep Orders are missing the corresponding Crown Court Outcomes</p>
	 * <h4>When</h4>
	 * <p>* The {@link FdcService#processDailyFiles()} method is called</p>
	 * <h4>Then:</h4>
	 * <p>1. The call to the callGlobalUpdate is successful i.e. MAAT API returned a successful response
	 * <p>2. The IDs of the 3 updated records are NOT returned.</p>
	 * <p>3. The updated IDs are NOT included in the list of IDs returned by the call to retrieve 'REQUESTED' FDC Contributions.</p>
	 * <p>4. The updated IDs are NOT included in the set of payloads sent to the DRC.</p>
	 * <p>5. After the `processDailyFiles` method call returns, the fdc_contribution entities corresponding to each
	 *       of the updated IDs is checked:<br>
	 *       - Each remains at status WAITING_ITEMS<br>
	 *       - Each has an unpopulated contribution_file ID.</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-407">DCES-407</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenDelayedPickupFdcContributionsWithMissingCCO_whenProcessDailyFilesRuns_thenTheirStatusIsNotUpdated() {
		final var updatedIds = spyFactory.createFdcDelayedPickupTestData(FdcTestType.NEGATIVE_CCO, 3);
		runProcessDailyFilesAndCheckResults(updatedIds, true, false, false, false, FdcContributionsStatus.WAITING_ITEMS);
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A negative FDC Contributions test which checks that FDC Contribution records do not get picked up for processing by the Fast Track pickup logic,
	 * 	if there are no Rep Order Crown Court Outcomes linked to the Rep Orders on which the FDC Contribution records are based.</p>
	 * <h4>Given:</h4>
	 * <p>* 3 fdc_contributions record IDs that would normally get picked up by the Fast Track pickup logic,
	 * 		but their Rep Orders are missing the corresponding Crown Court Outcomes</p>
	 * <h4>When</h4>
	 * <p>* The {@link FdcService#processDailyFiles()} method is called</p>
	 * <h4>Then:</h4>
	 * <p>1. The call to the callGlobalUpdate is successful i.e. MAAT API returned a successful response
	 * <p>2. The IDs of the 3 updated records are NOT returned.</p>
	 * <p>3. The updated IDs are NOT included in the list of IDs returned by the call to retrieve 'REQUESTED' FDC Contributions.</p>
	 * <p>4. The updated IDs are NOT included in the set of payloads sent to the DRC.</p>
	 * <p>5. After the `processDailyFiles` method call returns, the fdc_contribution entities corresponding to each
	 *       of the updated IDs is checked:<br>
	 *       - Each remains at status WAITING_ITEMS<br>
	 *       - Each has an unpopulated contribution_file ID.</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-408">DCES-408</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenFastTrackFdcContributionsWithMissingCCO_whenProcessDailyFilesRuns_thenTheirStatusIsNotUpdated() {
		final var updatedIds = spyFactory.createFastTrackTestData(FdcAccelerationType.POSITIVE, FdcTestType.NEGATIVE_CCO, 3);
		runProcessDailyFilesAndCheckResults(updatedIds, true, false, false, false, FdcContributionsStatus.WAITING_ITEMS);
	}

	/**
	 * Private method to run the method under test and check the outcome with the provided criteria
	 * @param updatedIds The IDs for the test data created before running the test (the Given bit)
	 * @param stubDrcSuccessFlag Do we want to stubbed DRC client to succeed or fail for this test
	 * @param updatedIdsShouldBeRequested Are the updated IDs expected to be requested for this test
	 * @param updatedIdsShouldBeSent Are the updated IDs expected to be sent for this test
	 * @param checkContributionFile Is the contribution file to be checked for this test
	 * @param fdcContributionsStatusExpected The status expected at the end of this test
	 */
	private void runProcessDailyFilesAndCheckResults(
			Set<Integer> updatedIds,
			boolean stubDrcSuccessFlag,
			boolean updatedIdsShouldBeRequested,
			boolean updatedIdsShouldBeSent,
			boolean checkContributionFile,
			FdcContributionsStatus fdcContributionsStatusExpected
	) {
		final FdcProcessSpy.FdcProcessSpyBuilder watching = spyFactory.newFdcProcessSpyBuilder()
				.traceExecuteFdcGlobalUpdate()
				.traceAndFilterGetFdcContributions(updatedIds)
				.traceAndStubSendFdcUpdate(id -> stubDrcSuccessFlag)
				.traceUpdateFdcs();

		// Call the processDailyFiles() method under test:
		fdcService.processDailyFiles();

		final FdcProcessSpy watched = watching.build();

		// Fetch some items of information from the maat-api to use during validation:
		final var fdcContributions = updatedIds.stream().map(testDataClient::getFdcContribution).toList();

		softly.assertThat(watched.getGlobalUpdateResponse().isSuccessful()).isTrue();
		softly.assertThat(updatedIds).hasSize(3).doesNotContainNull();

		if (updatedIdsShouldBeRequested)
			softly.assertThat(watched.getRequestedIds()).containsAll(updatedIds);
		else
			softly.assertThat(watched.getRequestedIds()).doesNotContainAnyElementsOf(updatedIds);

		if (updatedIdsShouldBeSent)
			softly.assertThat(watched.getSentIds()).containsAll(updatedIds);
		else
			softly.assertThat(watched.getSentIds()).doesNotContainAnyElementsOf(updatedIds);

		if (checkContributionFile) {
			if (watched.getRecordsSent() != 0) {
				// contribution_file got created:
				softly.assertThat(watched.getRecordsSent()).isPositive();
				softly.assertThat(watched.getXmlCcIds()).doesNotContainAnyElementsOf(updatedIds);
				softly.assertThat(watched.getXmlFileName()).isNotBlank();
				softly.assertThat(watched.getXmlFileResult()).isNotNull();
			} else {
				softly.assertThat(watched.getXmlCcIds()).isNull();
				softly.assertThat(watched.getXmlFileName()).isNull();
				softly.assertThat(watched.getXmlFileResult()).isNull();
			}
		}
		fdcContributions.forEach(fdcContribution -> {
			softly.assertThat(fdcContribution.getStatus()).isEqualTo(fdcContributionsStatusExpected);
			if (!checkContributionFile)
				softly.assertThat(fdcContribution.getContFileId()).isNull();
		});
	}

		fdcContributions.forEach(fdcContribution -> // 6.
			softly.assertThat(fdcContribution.getStatus()).isEqualTo(FdcContributionsStatus.REQUESTED));
	}

	/**
	 * <h4>Scenario:</h4>
	 * <p>A negative integration test to check that rep orders having a null SOD (Sentence Order Date) are NOT picked up and are not
	 *    processed.</p>
	 * <h4>Given:</h4>
	 * <p>* 3 fdc_contributions record IDs that have been updated to have SOD 3 months in the future.</p>
	 * <h4>When</h4>
	 * <p>* The {@link FdcService#processDailyFiles()} method is called.</p>
	 * <h4>Then:</h4>
	 * <p>1. The call to the callGlobalUpdate is successful i.e. MAAT API returned a successful response
	 * <p>2. The IDs of the 3 updated records are returned.</p>
	 * <p>3. The updated IDs are NOT included in the list of IDs returned by the call to retrieve 'REQUESTED' FDC Contributions.</p>
	 * <p>4. The updated IDs are NOT included in the set of payloads sent to the DRC.</p>
	 * <p>5. After the `processDailyFiles` method call returns, the fdc_contribution entities corresponding to each
	 *       of the updated IDs is checked:<br>
	 *       - Each remains at status SENT<br>
	 *       - Each has an unpopulated contribution_file ID.</p>
	 *
	 * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-360">DCES-405</a> for test specification.
	 */
	//TODO: Fix test with implementation of /assessment/ endpoint access.
//	@Disabled("Pending creation of /assessment/ handler")
	@Test
	void givenSomeFdcContributionsWithNullSOD_whenProcessDailyFilesRuns_thenTheyAreNotQueriedNotSentNorInCreatedFile() {
		// Set up test data for the scenario:
		final var updatedIds = spyFactory.createFdcDelayedPickupTestData(FdcTestType.NEGATIVE_SOD, 3);

		final FdcProcessSpy.FdcProcessSpyBuilder watching = spyFactory.newFdcProcessSpyBuilder()
				.traceExecuteFdcGlobalUpdate()
				.traceAndFilterGetFdcContributions(updatedIds)
				.traceAndStubSendFdcUpdate(id -> Boolean.TRUE)
				.traceUpdateFdcs();

		// Call the processDailyFiles() method under test:
		fdcService.processDailyFiles();

		final FdcProcessSpy watched = watching.build();

		// Fetch some items of information from the maat-api to use during validation:
		final var fdcContributions = updatedIds.stream().map(testDataClient::getFdcContribution).toList();

		softly.assertThat(watched.getGlobalUpdateResponse().isSuccessful()).isTrue(); // 1
		softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 2.
		softly.assertThat(watched.getRequestedIds()).doesNotContainAnyElementsOf(updatedIds); // 3.
		softly.assertThat(watched.getSentIds()).doesNotContainAnyElementsOf(updatedIds); // 4.

		fdcContributions.forEach(fdcContribution -> { // 5.
			softly.assertThat(fdcContribution.getStatus()).isEqualTo(FdcContributionsStatus.SENT);
			softly.assertThat(fdcContribution.getContFileId()).isNull();
		});
	}

}
