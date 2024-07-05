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
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.testing.SpyFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContributionIntegrationTest {
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Autowired
    private SpyFactory spyFactory;

    @Autowired
    private ContributionService contributionService;

    @Autowired
    private TestDataClient testDataClient;

    @AfterEach
    void assertAll() {
        softly.assertAll();
    }

    @Test
    void testProcessContributionUpdateWhenNotFound() {
        String errorText = "The request has failed to process";
        UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
                .concorId(9)
                .errorText(errorText)
                .build();
        String response = contributionService.processContributionUpdate(dataRequest);
        softly.assertThat(response).isEqualTo("The request has failed to process");
    }

    @Test
    void testProcessContributionUpdateWhenFound() {
        String errorText = "Error Text updated successfully.";
        UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
                .concorId(47959912)
                .errorText(errorText)
                .build();
        String response = contributionService.processContributionUpdate(dataRequest);
        softly.assertThat(response).isEqualTo("The request has been processed successfully");
    }

    /**
     * <h4>Scenario:</h4>
     * <p>A positive integration test which checks that ACTIVE concor_contribution records are picked up and processed
     *    correctly by the daily job that sends data to the DRC.</p>
     * <h4>Given:</h4>
     * <p>* Update 3 concor_contribution records to the ACTIVE status for the purposes of the test.</p>
     * <h4>When:</h4>
     * <p>* The {@link ContributionService#processDailyFiles()} method is called.</p>
     * <h4>Then:</h4>
     * <p>1. The IDs of the 3 updated records are returned.</p>
     * <p>2. The updated IDs are included in the list of IDs returned by `processDailyFiles`'s call to retrieve ACTIVE
     *    concor_contributions.</p>
     * <p>3. The updated IDs are included in the set of payloads sent to the DRC.</p>
     * <p>4. A contribution file gets created, with at least 3 successful records and 0 failed records.<br>
     *    The generated XML content includes each of the 3 linked rep_order IDs in a &lt;maat_id&gt; element tag.</p>
     * <p>5. After the `processDailyFiles` method call returns, the concor_contribution entities corresponding to each
     *    of the updated IDs is checked:<br>
     *    - Each now has status SENT<br>
     *    - Each now has a valid contribution_file ID<br>
     *    - Each now has a last modified user of "DCES"<br>
     *    - Each now has a last modified time during the method call.</p>
     * <p>6. After the `processDailyFiles` method call returns, the contribution_file entity that was created is
     *    checked:<br>
     *    - It has the expected filename<br>
     *    - It has a "records sent" of greater than or equal to 3<br>
     *    - It has a created and last modified user of "DCES"<br>
     *    - It has a created, sent and last modified time during the method call<br>
     *    - The fetched XML content includes each of the 3 linked rep_order IDs in a &lt;maat_id&gt; element tag.</p>
     *
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-349">DCES-349</a> for test specification.
     */
    @Test
    void givenSomeActiveConcorContributions_whenProcessDailyFilesRuns_thenTheyAreQueriedSentAndInCreatedFile() {
        // Update at least 3 concor_contribution rows to ACTIVE:
        final var updatedIds = spyFactory.updateConcorContributionStatus(ConcorContributionStatus.ACTIVE, 3);

        final var watching = spyFactory.newContributionProcessSpyBuilder();
        watching.instrumentAndFilterGetContributionsActive(updatedIds);
        watching.instrumentAndStubSendContributionUpdate(Boolean.TRUE);
        watching.instrumentUpdateContributions();

        // Call the processDailyFiles() method under test:
        final var startDate = LocalDate.now();
        contributionService.processDailyFiles();
        final var endDate = LocalDate.now();

        final var watched = watching.build();

        // Fetch some items of information from the maat-api to use during validation:
        final var concorContributions = updatedIds.stream().map(testDataClient::getConcorContribution).toList();
        final int contributionFileId = watched.getXmlFileResult();
        final var contributionFile = testDataClient.getContributionFile(contributionFileId);

        softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 1.
        softly.assertThat(watched.getActiveIds()).containsAll(updatedIds); // 2.
        softly.assertThat(watched.getSentIds()).containsAll(updatedIds); // 3.

        softly.assertThat(watched.getRecordsSent()).isGreaterThanOrEqualTo(3); // 4.
        softly.assertThat(watched.getXmlCcIds()).containsAll(updatedIds);
        concorContributions.forEach(concorContribution ->
                softly.assertThat(watched.getXmlContent()).contains("<maat_id>" + concorContribution.getRepId() + "</maat_id>"));

        concorContributions.forEach(concorContribution -> { // 5.
            softly.assertThat(concorContribution.getStatus()).isEqualTo(ConcorContributionStatus.SENT);
            softly.assertThat(concorContribution.getContribFileId()).isEqualTo(contributionFileId);
            softly.assertThat(concorContribution.getUserModified()).isEqualTo("DCES");
            softly.assertThat(concorContribution.getDateModified()).isBetween(startDate, endDate);
        });

        softly.assertThat(contributionFile.getXmlFileName()).isEqualTo(watched.getXmlFileName()); //6.
        softly.assertThat(contributionFile.getId()).isEqualTo(watched.getXmlFileResult());
        softly.assertThat(contributionFile.getRecordsSent()).isGreaterThanOrEqualTo(3);
        softly.assertThat(contributionFile.getDateCreated()).isBetween(startDate, endDate);
        softly.assertThat(contributionFile.getUserCreated()).isEqualTo("DCES");
        // uncomment after fix null actual: softly.assertThat(contributionFile.getDateModified()).isBetween(startDate, endDate);
        // uncomment after fix null actual: softly.assertThat(contributionFile.getUserModified()).isEqualTo("DCES");
        softly.assertThat(contributionFile.getDateSent()).isBetween(startDate, endDate);
        concorContributions.forEach(concorContribution ->
                softly.assertThat(contributionFile.getXmlContent()).contains("<maat_id>" + concorContribution.getRepId() + "</maat_id>"));
    }

    /**
     * <h4>Scenario:</h4>
     * <p>A negative integration test which checks that REPLACED and SENT concor_contribution records are not picked up
     *    and processed by the daily job that sends data to the DRC.</p>
     * <h4>Given:</h4>
     * <p>* Update 1 concor_contribution record to the REPLACED status for the purposes of the test.<br>
     *    * Update 1 concor_contribution record to the SENT status for the purposes of the test.</p>
     * <h4>When:</h4>
     * <p>* The {@link ContributionService#processDailyFiles()} method is called.</p>
     * <h4>Then:</h4>
     * <p>1. The IDs of the 2 updated records are returned.</p>
     * <p>2. The updated IDs are NOT included in the list of IDs returned by `processDailyFiles`'s call to retrieve
     *    ACTIVE concor_contributions.</p>
     * <p>3. The updated IDs are NOT included in the set of payloads sent to the DRC.</p>
     * <p>4. After the `processDailyFiles` method call returns, the concor_contribution entities corresponding to each
     *    of the updated IDs is checked:<br>
     *    - Each has unchanged status (REPLACED or SENT)<br>
     * <p>5. After the `processDailyFiles` method call returns, a contribution_file entity may or may not be created
     *    (depends if there are other ACTIVE records or not). If one is created, the updated IDs are not included.</p>
     *
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-351">DCES-351</a> for test specification.
     */
    @Test
    void givenAReplacedAndSentConcorContribution_whenProcessDailyFilesRuns_thenTheyAreNotQueriedNotSentNorInCreatedFile() {
        // Update at least 1 concor_contribution row to REPLACED, and leave 1 other at SENT:
        final var replacedIds = spyFactory.updateConcorContributionStatus(ConcorContributionStatus.REPLACED, 1);
        final var sentIds = spyFactory.updateConcorContributionStatus(ConcorContributionStatus.SENT, 1);
        final var updatedIds = Stream.of(replacedIds, sentIds).flatMap(List::stream).toList();

        final var watching = spyFactory.newContributionProcessSpyBuilder();
        watching.instrumentAndFilterGetContributionsActive(updatedIds);
        watching.instrumentAndStubSendContributionUpdate(Boolean.TRUE);
        watching.instrumentUpdateContributions();

        // Call the processDailyFiles() method under test:
        contributionService.processDailyFiles();

        final var watched = watching.build();

        // Fetch some items of information from the maat-api to use during validation:
        final var concorContributions = updatedIds.stream().map(testDataClient::getConcorContribution).toList();

        softly.assertThat(updatedIds).hasSize(2).doesNotContainNull(); // 1.
        softly.assertThat(watched.getActiveIds()).doesNotContainAnyElementsOf(updatedIds); // 2.
        softly.assertThat(watched.getSentIds()).doesNotContainAnyElementsOf(updatedIds); // 3.

        concorContributions.forEach(concorContribution -> // 4.
                softly.assertThat(concorContribution.getStatus()).isIn(ConcorContributionStatus.REPLACED, ConcorContributionStatus.SENT));

        if (!watched.getActiveIds().isEmpty()) { // 5.
            // contribution_file got created:
            softly.assertThat(watched.getRecordsSent()).isPositive();
            softly.assertThat(watched.getXmlCcIds()).doesNotContainAnyElementsOf(updatedIds);
            softly.assertThat(watched.getXmlFileName()).isNotBlank();
            softly.assertThat(watched.getXmlFileResult()).isNotNull();
        } else {
            softly.assertThat(watched.getRecordsSent()).isZero();
            softly.assertThat(watched.getXmlCcIds()).isNull();
            softly.assertThat(watched.getXmlFileName()).isNull();
            softly.assertThat(watched.getXmlFileResult()).isNull();
        }
    }
}
