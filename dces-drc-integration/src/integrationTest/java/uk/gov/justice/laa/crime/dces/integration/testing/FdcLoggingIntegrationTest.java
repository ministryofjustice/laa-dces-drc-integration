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
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcContribution;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcTestType;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.service.spy.FdcLoggingProcessSpy;
import uk.gov.justice.laa.crime.dces.integration.service.spy.FdcProcessSpy;
import uk.gov.justice.laa.crime.dces.integration.service.spy.SpyFactory;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FdcLoggingIntegrationTest {
    private static final String ERROR_TEXT = "There was an error with this contribution. Please contact CCMT team.";

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Autowired
    private SpyFactory spyFactory;

    @Autowired
    private FdcService fdcService;

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
     * <p>A positive integration test which checks that when a fdc_contribution is sent to the DRC, processed, and
     *    acknowledged, the acknowledgement response from the DRC updates the contribution_file correctly.</p>
     * <h4>Given:</h4>
     * <p>* Update 3 fdc_contribution records to the REQUESTED status for the purposes of the test.</p>
     * <p>* The {@link FdcService#processDailyFiles()} method is called and a contribution_file is created.</p>
     * <h4>When:</h4>
     * <p>* Simulate the DRC calling our services to log their receipt of our fdc_contributions by calling the
     *    `/process-drc-update/fdc` endpoint once for each updated ID with a blank error text to indicate that
     *    there were no errors processing that record.</p>
     * <h4>Then:</h4>
     * <p>1. The IDs of the 3 updated records are returned.</p>
     * <p>2. The integration's steps are called correctly to create a contribution file, which is persisted.</p>
     * <p>3. The endpoint that the DRC calls returns this contribution file ID each time it is called.</p>
     * <p>4. After all three fdc_contributions have been acknowledged by the DRC, the contribution file is
     *    checked:<br>
     *    - It has a "records received" of 3<br>
     *    - It has a received and last modified time during the acknowledgements<br>
     *    - It has a last modified user of "DCES"</p>
     * <p>5. After all three fdc_contributions have been acknowledged by the DRC, the contribution file errors
     *    for the contribution file and each fdc_contribution are checked:<br>
     *    - Each should not find any records</p>
     *
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-362">DCES-362</a> for test specification.
     */
    @Disabled("Disabled as the controller has been temporarily removed.")
    @Test
    void givenSomeRequestedFdcContributionsAndProcessDailyFilesRan_whenDrcRespondsToAcknowledge_thenContributionsAndFileAreUpdated() {
        // Update at least 3 fdc_contribution rows to REQUESTED:
        final var updatedIds = spyFactory.createFdcDelayedPickupTestData(FdcTestType.POSITIVE, 3);

        final FdcProcessSpy.FdcProcessSpyBuilder watching = spyFactory.newFdcProcessSpyBuilder()
                .traceExecuteFdcGlobalUpdate()
                .traceAndFilterGetFdcContributions(updatedIds) // fake REQUESTED records to just 3 updated
                .traceAndStubSendFdcUpdate(id -> Boolean.TRUE) // fake DRC response
                .traceUpdateFdcs(); // capture contribution_file ID

        fdcService.processDailyFiles();

        final FdcProcessSpy watched = watching.build();

        final FdcLoggingProcessSpy.FdcLoggingProcessSpyBuilder logging = spyFactory.newFdcLoggingProcessSpyBuilder()
                .traceSendLogFdcProcessed();

        // Call the fake DRC processing-successful responses under test:
        final var startDate = LocalDate.now();
        updatedIds.forEach(this::successfulFdc);
        final var endDate = LocalDate.now();

        final FdcLoggingProcessSpy logged = logging.build();

        // Fetch some items of information from the maat-api to use during validation:
        final long contributionFileId = watched.getXmlFileResult();
        final var contributionFile = spyFactory.getContributionsFile(contributionFileId);
        final var contributionFileErrors = updatedIds.stream().flatMap(id ->
                spyFactory.getContributionFileErrorOptional(contributionFileId, id).stream()).toList();

        softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 1.
        softly.assertThat(contributionFile.getId()).isEqualTo(contributionFileId); // 2.

        softly.assertThat(logged.getContributionFileIds()).containsExactly(contributionFileId, contributionFileId, contributionFileId); // 3.
        softly.assertThat(logged.getFdcContributionIds()).containsOnlyOnceElementsOf(updatedIds);

        softly.assertThat(contributionFile.getRecordsReceived()).isEqualTo(3); // 4.
        softly.assertThat(contributionFile.getDateReceived()).isBetween(startDate, endDate);
        softly.assertThat(contributionFile.getDateModified()).isBetween(startDate, endDate);
        softly.assertThat(contributionFile.getUserModified()).isEqualTo("DCES");

        softly.assertThat(contributionFileErrors).isEmpty(); // 5.
    }

    /**
     * <h4>Scenario:</h4>
     * <p>A negative integration test which checks that when a fdc_contribution is sent to the DRC, but a response
     *    is received from the DRC to indicate it was not processed, then the contribution file is not updated and a
     *    record is created in contribution_file_errors.</p>
     * <h4>Given:</h4>
     * <p>* Update 3 fdc_contribution records to the REQUESTED status for the purposes of the test.</p>
     * <p>* The {@link FdcService#processDailyFiles()} method is called and a contribution_file is created.</p>
     * <h4>When:</h4>
     * <p>* Simulate the DRC calling our services to log that a fdc_contribution could not be processed by calling
     *      the `/process-drc-update/fdc` endpoint once for each updated ID with a populated error text to
     *      indicate that there was an error processing that record.</p>
     * <h4>Then:</h4>
     * <p>1. The IDs of the 3 updated records are returned.</p>
     * <p>2. The integration's steps are called correctly to create a contribution file, which is persisted.</p>
     * <p>3. The endpoint that the DRC calls returns this contribution file ID each time it is called.</p>
     * <p>4. After all three fdc_contributions have been marked as failed by the DRC, the contribution file is
     *       checked:<br>
     *       - It has either an unset (null) or zero (0) "records received"<br>
     *       - It has an unset (null) received time<br>
     *       - It has a last modified time during the processDailyFiles call<br>
     *       - It has a last modified user of "DCES"</p>
     * <p>5. After all three fdc_contributions have been acknowledged by the DRC, the contribution file errors
     *       for the contribution file and each fdc_contribution are checked:<br>
     *       - Each should return a contribution_file_error with the expected IDs.<br>
     *       - Each should have the correct rep_id<br>
     *       - Each should have the expected error text</p>
     *
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-363">DCES-363</a> for test specification.
     */
    @Disabled("Disabled as the controller has been temporarily removed.")
    @Test
    void givenSomeRequestedFdcContributionsAndProcessDailyFilesRan_whenDrcRespondsWithError_thenContributionFileIsNotUpdatedButErrorIsCreated() {
        // Update at least 3 fdc_contribution rows to REQUESTED:
        final var updatedIds = spyFactory.createFdcDelayedPickupTestData(FdcTestType.POSITIVE, 3);

        final FdcProcessSpy.FdcProcessSpyBuilder watching = spyFactory.newFdcProcessSpyBuilder()
                .traceExecuteFdcGlobalUpdate()
                .traceAndFilterGetFdcContributions(updatedIds) // fake REQUESTED records to just 3 updated
                .traceAndStubSendFdcUpdate(id -> Boolean.TRUE) // fake DRC response
                .traceUpdateFdcs(); // capture contribution_file ID

        fdcService.processDailyFiles();

        final FdcProcessSpy watched = watching.build();

        final FdcLoggingProcessSpy.FdcLoggingProcessSpyBuilder logging = spyFactory.newFdcLoggingProcessSpyBuilder()
                .traceSendLogFdcProcessed();

        // Call the fake DRC processing-failed responses under test:
        final var startDate = LocalDate.now();
        updatedIds.forEach(this::failedFdc);
        final var endDate = LocalDate.now();

        final FdcLoggingProcessSpy logged = logging.build();

        // Fetch some items of information from the maat-api to use during validation:
        final var repIds = updatedIds.stream().map(spyFactory::getFdcContribution).map(FdcContribution::getMaatId).toList();
        final long contributionFileId = watched.getXmlFileResult();
        final var contributionFile = spyFactory.getContributionsFile(contributionFileId);
        final var contributionFileErrors = updatedIds.stream().flatMap(id ->
                spyFactory.getContributionFileErrorOptional(contributionFileId, id).stream()).toList();

        softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 1.
        softly.assertThat(contributionFile.getId()).isEqualTo(contributionFileId); // 2.

        softly.assertThat(logged.getContributionFileIds()).containsExactly(contributionFileId, contributionFileId, contributionFileId); // 3.
        softly.assertThat(logged.getFdcContributionIds()).containsOnlyOnceElementsOf(updatedIds);

        softly.assertThat(contributionFile.getRecordsReceived()).isIn(0, null); // 4. (either zero or NULL)
        softly.assertThat(contributionFile.getDateReceived()).isNull();
        softly.assertThat(contributionFile.getDateModified()).isBeforeOrEqualTo(startDate);
        softly.assertThat(contributionFile.getUserModified()).isEqualTo("DCES");

        softly.assertThat(contributionFileErrors).hasSize(3); // 5.
        contributionFileErrors.forEach(contributionFileError -> {
            softly.assertThat(contributionFileError.getContributionFileId()).isEqualTo(contributionFileId);
            softly.assertThat(contributionFileError.getContributionId()).isIn(updatedIds);
            // uncomment when FdcContribution.getMaatId() works: softly.assertThat(contributionFileError.getRepId()).isIn(repIds);
            softly.assertThat(contributionFileError.getErrorText()).isEqualTo(ERROR_TEXT);
            softly.assertThat(contributionFileError.getConcorContributionId()).isNull();
            softly.assertThat(contributionFileError.getFdcContributionId()).isIn(updatedIds);
            softly.assertThat(contributionFileError.getDateCreated()).isBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        });
    }

    /**
     * Act like a DRC acknowledging a successful or failed contribution update. This test uses MockMVC to handle CSRF
     * dnd OAuth 2.0 login, then calls our own '/process-drc-update/contribution' endpoint like the DRC would.
     * <p>
     * Testing utility method.
     */
    private void acknowledgeFdc(final long fdcContributionId, final String errorText) throws Exception {
        final var request = FdcAckFromDrc.of(fdcContributionId, errorText);
        String json = mapper.writeValueAsString(request);
        mockMvc.perform(post("/api/dces/v1/fdc")
                        .with(csrf())
                        .with(oauth2Login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    private void successfulFdc(final long fdcContributionId) {
        try {
            acknowledgeFdc(fdcContributionId, null);
        } catch (Exception e) {
            softly.fail("successfulFdc(" + fdcContributionId + ") failed with an exception:", e);
        }
    }

    private void failedFdc(final long fdcContributionId) {
        try {
            acknowledgeFdc(fdcContributionId, ERROR_TEXT);
        } catch (Exception e) {
            softly.fail("failedFdc(" + fdcContributionId + ") failed with an exception:", e);
        }
    }
}
