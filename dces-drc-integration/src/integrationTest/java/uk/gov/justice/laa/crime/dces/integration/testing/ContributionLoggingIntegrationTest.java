package uk.gov.justice.laa.crime.dces.integration.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionResponseDTO;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionFileService;
import uk.gov.justice.laa.crime.dces.integration.service.spy.ContributionLoggingProcessSpy;
import uk.gov.justice.laa.crime.dces.integration.service.spy.ContributionProcessSpy;
import uk.gov.justice.laa.crime.dces.integration.service.spy.SpyFactory;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.justice.laa.crime.dces.integration.utils.IntTestDataFixtures.buildContribAck;

@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContributionLoggingIntegrationTest {
    private static final String ERROR_TEXT = "There was an error with this contribution. Please contact CCMT team.";

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Autowired
    private SpyFactory spyFactory;

    @Autowired
    private ContributionFileService contributionFileService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @AfterEach
    void assertAll() {
        softly.assertAll();
    }

    /**
     * <h4>Scenario:</h4>
     * <p>A positive integration test which checks that when a concor_contribution is sent to the DRC, processed, and
     * acknowledged, the acknowledgement response from the DRC updates the contribution_file correctly.</p>
     * <h4>Given:</h4>
     * <p>* Update 3 concor_contribution records to the ACTIVE status for the purposes of the test.</p>
     * <p>* The {@link ContributionFileService#processDailyFiles()} method is called and a contribution_file is created.</p>
     * <h4>When:</h4>
     * <p>* Simulate the DRC calling our services to log their receipt of our concor_contributions by calling the
     *    `/process-drc-update/contribution` endpoint once for each updated ID with a blank error text to indicate that
     *    there were no errors processing that record.</p>
     * <h4>Then:</h4>
     * <p>1. The IDs of the 3 updated records are returned.</p>
     * <p>2. The integration's steps are called correctly to create a contribution file, which is persisted.</p>
     * <p>3. The endpoint that the DRC calls returns this contribution file ID each time it is called.</p>
     * <p>4. After all three concor_contributions have been acknowledged by the DRC, the contribution file is
     *    checked:<br>
     *    - It has a "records received" of 3<br>
     *    - It has a received and last modified time during the acknowledgements<br>
     *    - It has a last modified user of "DCES"</p>
     * <p>5. After all three concor_contributions have been acknowledged by the DRC, the contribution file errors
     *    for the contribution file and each concor_contribution are checked:<br>
     *    - Each should not find any records</p>
     *
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-354">DCES-354</a> for test specification.
     */
    @Disabled("Disabled as the controller has been temporarily removed.")
    @Test
    void givenSomeActiveConcorContributionsAndProcessDailyFilesRan_whenDrcRespondsToAcknowledge_thenContributionsAndFileAreUpdated() {
        // Update at least 3 concor_contribution rows to ACTIVE:
        final var updatedIds = spyFactory.updateConcorContributionStatus(ConcorContributionStatus.ACTIVE, 3);

        final ContributionProcessSpy.ContributionProcessSpyBuilder watching = spyFactory.newContributionProcessSpyBuilder()
                .traceAndFilterGetContributionsActive(updatedIds) // fake ACTIVE records to just 3 updated
                .traceAndStubSendContributionUpdate(id -> Boolean.TRUE) // fake DRC response
                .traceUpdateContributions(); // capture contribution_file ID

        contributionFileService.processDailyFiles();

        final ContributionProcessSpy watched = watching.build();

        final ContributionLoggingProcessSpy.ContributionLoggingProcessSpyBuilder logging = spyFactory.newContributionLoggingProcessSpyBuilder()
                .traceSendLogContributionProcessed();

        // Call the fake DRC processing-successful responses under test:
        final var startDate = LocalDate.now();
        updatedIds.forEach(this::successfulContribution);
        final var endDate = LocalDate.now();

        final ContributionLoggingProcessSpy logged = logging.build();

        // Fetch some items of information from the maat-api to use during validation:
        final long contributionFileId = watched.getXmlFileResult();
        final var contributionFile = spyFactory.getContributionsFile(contributionFileId);
        final var contributionFileErrors = updatedIds.stream().flatMap(id ->
                spyFactory.getContributionFileErrorOptional(contributionFileId, id).stream()).toList();

        softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 1.
        softly.assertThat(contributionFile.getId()).isEqualTo(contributionFileId); // 2.

        softly.assertThat(logged.getContributionFileIds()).containsExactly(contributionFileId, contributionFileId, contributionFileId); // 3.
        softly.assertThat(logged.getConcorContributionIds()).containsOnlyOnceElementsOf(updatedIds);

        softly.assertThat(contributionFile.getRecordsReceived()).isEqualTo(3); // 4.
        softly.assertThat(contributionFile.getDateReceived()).isBetween(startDate, endDate);
        softly.assertThat(contributionFile.getDateModified()).isBetween(startDate, endDate);
        softly.assertThat(contributionFile.getUserModified()).isEqualTo("DCES");

        softly.assertThat(contributionFileErrors).isEmpty(); // 5.
    }

    /**
     * <h4>Scenario:</h4>
     * <p>A negative integration test which checks that when a concor_contribution is sent to the DRC, but a response
     * is received from the DRC to indicate it was not processed, then the contribution file is not updated and a record
     * is created in contribution_file_errors.</p>
     * <h4>Given:</h4>
     * <p>* Update 3 concor_contribution records to the ACTIVE status for the purposes of the test.</p>
     * <p>* The {@link ContributionFileService#processDailyFiles()} method is called and a contribution_file is created.</p>
     * <h4>When:</h4>
     * <p>* Simulate the DRC calling our services to log that a concor_contribution could not be processed by calling
     *    the `/process-drc-update/contribution` endpoint once for each updated ID with a populated error text to
     *    indicate that there was an error processing that record.</p>
     * <h4>Then:</h4>
     * <p>1. The IDs of the 3 updated records are returned.</p>
     * <p>2. The integration's steps are called correctly to create a contribution file, which is persisted.</p>
     * <p>3. The endpoint that the DRC calls returns this contribution file ID each time it is called.</p>
     * <p>4. After all three concor_contributions have been marked as failed by the DRC, the contribution file is
     *    checked:<br>
     *    - It has either an unset (null) or zero (0) "records received"<br>
     *    - It has an unset (null) received time<br>
     *    - It has a last modified time during the processDailyFiles call<br>
     *    - It has a last modified user of "DCES"</p>
     * <p>5. After all three concor_contributions have been acknowledged by the DRC, the contribution file errors
     *    for the contribution file and each concor_contribution are checked:<br>
     *    - Each should return a contribution_file_error with the expected IDs.<br>
     *    - Each should have the correct rep_id<br>
     *    - Each should have the expected error text</p>
     *
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-355">DCES-355</a> for test specification.
     */
    @Disabled("Disabled as the controller has been temporarily removed.")
    @Test
    void givenSomeActiveConcorContributionsAndProcessDailyFilesRan_whenDrcRespondsWithError_thenContributionFileIsNotUpdatedButErrorIsCreated() {
        // Update at least 3 concor_contribution rows to ACTIVE:
        final var updatedIds = spyFactory.updateConcorContributionStatus(ConcorContributionStatus.ACTIVE, 3);

        final ContributionProcessSpy.ContributionProcessSpyBuilder watching = spyFactory.newContributionProcessSpyBuilder()
                .traceAndFilterGetContributionsActive(updatedIds) // fake ACTIVE records to just 3 updated
                .traceAndStubSendContributionUpdate(id -> Boolean.TRUE) // fake DRC response
                .traceUpdateContributions(); // capture contribution_file ID

        contributionFileService.processDailyFiles();

        final ContributionProcessSpy watched = watching.build();

        final ContributionLoggingProcessSpy.ContributionLoggingProcessSpyBuilder logging = spyFactory.newContributionLoggingProcessSpyBuilder()
                .traceSendLogContributionProcessed();

        // Call the fake DRC processing-failed responses under test:
        final var startDate = LocalDate.now();
        updatedIds.forEach(this::failedContribution);
        final var endDate = LocalDate.now();

        final ContributionLoggingProcessSpy logged = logging.build();

        // Fetch some items of information from the maat-api to use during validation:
        final var repIds = updatedIds.stream().map(spyFactory::getConcorContribution).map(ConcorContributionResponseDTO::getRepId).toList();
        final long contributionFileId = watched.getXmlFileResult();
        final var contributionFile = spyFactory.getContributionsFile(contributionFileId);
        final var contributionFileErrors = updatedIds.stream().flatMap(id ->
                spyFactory.getContributionFileErrorOptional(contributionFileId, id).stream()).toList();

        softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 1.
        softly.assertThat(contributionFile.getId()).isEqualTo(contributionFileId); // 2.

        softly.assertThat(logged.getContributionFileIds()).containsExactly(contributionFileId, contributionFileId, contributionFileId); // 3.
        softly.assertThat(logged.getConcorContributionIds()).containsOnlyOnceElementsOf(updatedIds);

        softly.assertThat(contributionFile.getRecordsReceived()).isIn(0, null); // 4. (either zero or NULL)
        softly.assertThat(contributionFile.getDateReceived()).isNull();
        softly.assertThat(contributionFile.getDateModified()).isBeforeOrEqualTo(startDate);
        softly.assertThat(contributionFile.getUserModified()).isEqualTo("DCES");

        softly.assertThat(contributionFileErrors).hasSize(3); // 5.
        contributionFileErrors.forEach(contributionFileError -> {
            softly.assertThat(contributionFileError.getContributionFileId()).isEqualTo(contributionFileId);
            softly.assertThat(contributionFileError.getContributionId()).isIn(updatedIds);
            softly.assertThat(contributionFileError.getRepId()).isIn(repIds);
            softly.assertThat(contributionFileError.getErrorText()).isEqualTo(ERROR_TEXT);
            softly.assertThat(contributionFileError.getConcorContributionId()).isIn(updatedIds);
            softly.assertThat(contributionFileError.getFdcContributionId()).isNull();
            softly.assertThat(contributionFileError.getDateCreated()).isBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        });
    }

    /**
     * Act like a DRC acknowledging a successful or failed contribution update. This test uses MockMVC to handle CSRF
     * dnd OAuth 2.0 login, then calls our own '/process-drc-update/contribution' endpoint like the DRC would.
     * <p>
     * Testing utility method.
     */
    private void acknowledgeContribution(final long concorContributionId, final String errorText) throws Exception {
        final var request = buildContribAck(concorContributionId, errorText);
        String json = mapper.writeValueAsString(request);
        mockMvc.perform(post("/api/dces/v1/contribution")
                        .with(csrf())
                        .with(oauth2Login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    private void successfulContribution(final long concorContributionId) {
        try {
            acknowledgeContribution(concorContributionId, null);
        } catch (Exception e) {
            softly.fail("successfulContribution(" + concorContributionId + ") failed with an exception:", e);
        }
    }

    private void failedContribution(final long concorContributionId) {
        try {
            acknowledgeContribution(concorContributionId, ERROR_TEXT);
        } catch (Exception e) {
            softly.fail("failedContribution(" + concorContributionId + ") failed with an exception:", e);
        }
    }
}
