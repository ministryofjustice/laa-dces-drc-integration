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
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionResponseDTO;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateConcorContributionStatusRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.testing.SpyFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

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

    @Autowired
    private ContributionClient contributionClient;

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

    /** See DCES-349 for test specification. */
    @Test
    void testActiveContributionIsProcessed() {
        final var watching = spyFactory.newContributionProcessSpyBuilder();
        watching.instrumentGetContributionsActive();
        watching.instrumentStubbedSendContributionUpdate(Boolean.TRUE);
        watching.instrumentUpdateContributions();

        // Create at least 3 ACTIVE concor_contribution rows for this test to be meaningful:
        final var updatedIds = updateConcorContributionStatus(ConcorContributionStatus.ACTIVE, 3);
        watching.updatedIds(updatedIds);

        // Call the processDailyFiles() method under test (date range in case executing near to midnight)
        final var startDate = LocalDate.now();
        contributionService.processDailyFiles();
        final var endDate = LocalDate.now();

        // Fetch some items of information from the maat-api to use during validation:
        updatedIds.forEach(id -> watching.concorContribution(testDataClient.getConcorContribution(id)));
        contributionClient.findContributionFiles(startDate, endDate).stream() // retrieve the contribution_file row's content
                .filter(content -> content.contains("<filename>" + watching.getXmlFileName() + "</filename>"))
                .findFirst().ifPresentOrElse(watching::contributionFileContent,
                        () -> softly.fail("no contribution_file named `" + watching.getXmlFileName() + "` was found"));
        final var watched = watching.build(); // it's solely validation from now on

        softly.assertThat(watched.getUpdatedIds()).hasSize(3);
        softly.assertThat(watched.getActiveIds()).containsAll(watched.getUpdatedIds());
        softly.assertThat(watched.getSentIds()).containsAll(watched.getUpdatedIds());

        softly.assertThat(watched.getRecordsSent()).isGreaterThanOrEqualTo(3);
        softly.assertThat(watched.getXmlCcIds()).containsAll(watched.getUpdatedIds());
        watched.getConcorContributions().forEach(concorContribution ->
                softly.assertThat(watched.getXmlContent()).contains("<maat_id>" + concorContribution.getRepId() + "</maat_id>"));
        softly.assertThat(watched.getXmlFileName()).isNotBlank();
        softly.assertThat(watched.getXmlFileResult()).isNotNull();
        final int contributionFileId = watched.getXmlFileResult();

        checkConcorContributionsAreSent(watched.getConcorContributions(), startDate, endDate, contributionFileId);

        checkContributionFileIsCreated(watched.getContributionFileContent(), startDate, endDate);
        watched.getConcorContributions().forEach(concorContribution ->
                softly.assertThat(watched.getContributionFileContent()).contains("<maat_id>" + concorContribution.getRepId() + "</maat_id>"));
    }

    /** See DCES-361 for test specification. */
    @Test
    void testReplacedContributionIsNotProcessed() {
        final var watching = spyFactory.newContributionProcessSpyBuilder();
        watching.instrumentGetContributionsActive();
        watching.instrumentStubbedSendContributionUpdate(Boolean.TRUE);
        watching.instrumentUpdateContributions();

        // Create at least 1 REPLACED concor_contribution row for this test to be meaningful:
        final var updatedIds = updateConcorContributionStatus(ConcorContributionStatus.REPLACED, 1);
        watching.updatedIds(updatedIds);

        // Call the processDailyFiles() method under test
        contributionService.processDailyFiles();

        // Fetch some items of information from the maat-api to use during validation:
        updatedIds.forEach(id -> watching.concorContribution(testDataClient.getConcorContribution(id)));
        final var watched = watching.build(); // it's solely validation from now on

        softly.assertThat(watched.getUpdatedIds()).hasSize(1);
        softly.assertThat(watched.getActiveIds()).doesNotContainAnyElementsOf(watched.getUpdatedIds());
        softly.assertThat(watched.getSentIds()).doesNotContainAnyElementsOf(watched.getUpdatedIds());

        if (!watched.getActiveIds().isEmpty()) { // are there are any other ACTIVE records or not?
            softly.assertThat(watched.getRecordsSent()).isPositive();
            softly.assertThat(watched.getXmlCcIds()).doesNotContainAnyElementsOf(watched.getUpdatedIds());
            softly.assertThat(watched.getXmlFileName()).isNotBlank();
            softly.assertThat(watched.getXmlFileResult()).isNotNull();
        } else {
            softly.assertThat(watched.getRecordsSent()).isZero();
            softly.assertThat(watched.getXmlCcIds()).isNull();
            softly.assertThat(watched.getXmlFileName()).isNull();
            softly.assertThat(watched.getXmlFileResult()).isNull();
        }

        watched.getConcorContributions().forEach(concorContribution ->
                softly.assertThat(concorContribution.getStatus()).isEqualTo(ConcorContributionStatus.REPLACED));
    }

    private void checkConcorContributionsAreSent(final List<ConcorContributionResponseDTO> concorContributions, final LocalDate startDate, final LocalDate endDate, final int contributionFileId) {
        concorContributions.forEach(concorContribution -> {
            softly.assertThat(concorContribution.getStatus()).isEqualTo(ConcorContributionStatus.SENT);
            softly.assertThat(concorContribution.getContribFileId()).isEqualTo(contributionFileId);
            softly.assertThat(concorContribution.getUserModified()).isEqualTo("DCES"); // actually we should be checking for "DCES"
            softly.assertThat(concorContribution.getDateModified()).isBetween(startDate, endDate);
        });
    }

    private void checkContributionFileIsCreated(final String content, final LocalDate startDate, final LocalDate endDate) {
        var matcher = Pattern.compile("<dateGenerated>([^<]*)</dateGenerated>").matcher(content);
        if (matcher.find()) {
            softly.assertThat(LocalDate.parse(matcher.group(1))).isBetween(startDate, endDate);
        } else {
            softly.fail("contribution_file does not contain a <dateGenerated> element");
        }
        matcher = Pattern.compile("<recordCount>([^<]*)</recordCount>").matcher(content);
        if (matcher.find()) {
            softly.assertThat(Integer.parseInt(matcher.group(1))).isGreaterThanOrEqualTo(3);
        } else {
            softly.fail("contribution_file does not contain a <recordCount> element");
        }
    }

    private List<Integer> updateConcorContributionStatus(final ConcorContributionStatus status, final int recordCount) {
        UpdateConcorContributionStatusRequest dataRequest = UpdateConcorContributionStatusRequest.builder()
                .status(status)
                .recordCount(recordCount)
                .build();
        return testDataClient.updateConcorContributionStatus(dataRequest);
    }
}
