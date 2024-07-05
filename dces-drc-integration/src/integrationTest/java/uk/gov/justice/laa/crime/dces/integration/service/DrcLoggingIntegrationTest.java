package uk.gov.justice.laa.crime.dces.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionFileErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.testing.SpyFactory;

import java.time.LocalDate;
import java.util.Optional;

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
class DrcLoggingIntegrationTest {
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Autowired
    private SpyFactory spyFactory;

    @Autowired
    private ContributionService contributionService;

    @Autowired
    private TestDataClient testDataClient;

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
     * <p>* The {@link ContributionService#processDailyFiles()} method is called and a contribution_file is created.</p>
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
    @Test
    void givenSomeActiveConcorContributionsAndProcessDailyFilesRan_whenDrcRespondsToAcknowledge_thenContributionsAndFileAreUpdated() {
        // Update at least 3 concor_contribution rows to ACTIVE:
        final var updatedIds = spyFactory.updateConcorContributionStatus(ConcorContributionStatus.ACTIVE, 3);

        final var watching = spyFactory.newContributionProcessSpyBuilder();
        watching.instrumentAndFilterGetContributionsActive(updatedIds); // fake ACTIVE records to just 3 updated
        watching.instrumentAndStubSendContributionUpdate(Boolean.TRUE); // fake DRC response
        watching.instrumentUpdateContributions(); // capture contribution_file ID

        contributionService.processDailyFiles();

        final var watched = watching.build();

        final var logging = spyFactory.newDrcLoggingProcessSpyBuilder();
        logging.instrumentSendLogContributionProcessed();

        // Call the fake DRC responses under test:
        final var startDate = LocalDate.now();
        updatedIds.forEach(this::acknowledgeSuccessContributionMVC);
        final var endDate = LocalDate.now();

        final var logged = logging.build();

        // Fetch some items of information from the maat-api to use during validation:
        final int contributionFileId = watched.getXmlFileResult();
        final var contributionFileResponse = testDataClient.getContributionFile(contributionFileId);
        final var contributionFileErrorResponses = updatedIds.stream().flatMap(id ->
                getContributionFileErrorOptional(contributionFileId, id).stream()).toList();

        softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 1.
        softly.assertThat(contributionFileResponse.getId()).isEqualTo(contributionFileId); // 2.

        softly.assertThat(logged.getContributionFileIds()).containsExactly(contributionFileId, contributionFileId, contributionFileId); // 3.
        softly.assertThat(logged.getConcorContributionIds()).containsOnlyOnceElementsOf(updatedIds);

        softly.assertThat(contributionFileResponse.getRecordsReceived()).isEqualTo(3); // 4.
        softly.assertThat(contributionFileResponse.getDateReceived()).isBetween(startDate, endDate);
        softly.assertThat(contributionFileResponse.getDateModified()).isBetween(startDate, endDate);
        softly.assertThat(contributionFileResponse.getUserModified()).isEqualTo("DCES");

        softly.assertThat(contributionFileErrorResponses).isEmpty(); // 5.
    }

    /**
     * Act like a DRC acknowledging a successful contribution update. This test starts up the application listening on
     * a random part number, then calls its '/process-drc-update/contribution' endpoint like the DRC would.
     * <p>
     * Testing utility method.
     */
    private void acknowledgeSuccessContributionMVC(final int concorContributionId) {
        try {
            final var request = UpdateLogContributionRequest.builder().concorId(concorContributionId).errorText(null).build();
            String json = mapper.writeValueAsString(request);
            mockMvc.perform(post("/api/internal/v1/dces-drc-integration/process-drc-update/contribution")
                            .with(csrf())
                            .with(oauth2Login())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(content().string("The request has been processed successfully"));
        } catch (Exception e) {
            softly.fail("acknowledgeContributionMVC(" + concorContributionId + ") failed", e);
        }
    }

    /*
    // Need to handle CSRF and OAuth2 login before can use testRestTemplate like this:
    private void acknowledgeSuccessContributionHTTP(final int concorContributionId) {
        try {
            final var request = UpdateLogContributionRequest.builder().concorId(concorContributionId).errorText(null).build();
            final var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            final var requestEntity = new HttpEntity<>(request, headers);
            final var responseEntity = testRestTemplate.postForEntity(
                    "http://localhost:" + port + "/api/internal/v1/dces-drc-integration/process-drc-update/contribution",
                    requestEntity, String.class);
            softly.assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
            softly.assertThat(responseEntity.getBody()).isEqualTo("The request has been processed successfully");
        } catch (Exception e) {
            softly.fail("acknowledgeContributionHTTP(" + concorContributionId + ") failed", e);
        }
    }
     */

    /**
     * Get a contribution_file_error entity, but handle 404 by returning Optional.empty() instead of an exception.
     * <p>
     * Testing utility method.
     */
    private Optional<ContributionFileErrorResponse> getContributionFileErrorOptional(final int contributionFileId, final int contributionId) {
        try {
            return Optional.of(testDataClient.getContributionFileError(contributionFileId, contributionId));
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }
}
