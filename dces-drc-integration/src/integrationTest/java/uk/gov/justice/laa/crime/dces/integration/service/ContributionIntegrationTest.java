package uk.gov.justice.laa.crime.dces.integration.service;

import lombok.Builder;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
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
    void assertAll() {
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

    /**
     * @param updatedIds              Returned from maat-api by TestDataClient.updateConcorContributionStatus(...)
     * @param activeIds               Returned from maat-api by ContributionClient.getContributions("ACTIVE")
     * @param sentIds                 Sent to the DRC by DrcClient.sendContributionUpdate(...)
     * @param xmlCcIds                Sent to maat-api by ContributionClient.updateContribution(...)
     * @param recordsSent              "    "    "
     * @param xmlContent               "    "    "
     * @param xmlFileName              "    "    "
     * @param xmlFileResult           Returned from maat-api by ContributionClient.updateContribution(...)
     * @param concurContributions     Returned from maat-api by TestDataClient.getContribution(...)
     * @param contributionFileContent Returned from maat-api by ContributionClient.findContributionFiles(...)
     */
    @Builder(setterPrefix = "with")
    record ContributionProcess(@Singular List<Integer> updatedIds,
                               @Singular Set<Integer> activeIds,
                               @Singular Set<Integer> sentIds,
                               @Singular Set<Integer> xmlCcIds,
                               int recordsSent,
                               String xmlContent,
                               String xmlFileName,
                               Boolean xmlFileResult,
                               @Singular List<ConcorContributionResponseDTO> concurContributions,
                               String contributionFileContent) {
        public static class ContributionProcessBuilder { // Add a method to the Lombok-generated builder:
            public String xmlFileName() {
                return xmlFileName;
            }
        }
    }

    @Test
    void testPositiveActiveContributionProcess() {
        final var processing = ContributionProcess.builder();

        doAnswer(invocation -> {
            // Because ContributionClient is a proxied interface, cannot just call `invocation.callRealMethod()` here.
            // https://github.com/spring-projects/spring-boot/issues/36653
            @SuppressWarnings("unchecked") var result = (List<ConcurContribEntry>) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
            processing.withActiveIds(result.stream().map(ConcurContribEntry::getConcorContributionId).collect(Collectors.toSet()));
            return result;
        }).when(contributionClientSpy).getContributions("ACTIVE");

        doAnswer(invocation -> {
            processing.withSentId(((SendContributionFileDataToDrcRequest) invocation.getArgument(0)).getContributionId());
            return Boolean.TRUE;
        }).when(drcClientSpy).sendContributionUpdate(any());

        doAnswer(invocation -> {
            var data = (ContributionUpdateRequest) invocation.getArgument(0);
            processing.withXmlCcIds(data.getConcorContributionIds().stream().map(Integer::valueOf).collect(Collectors.toSet()));
            processing.withRecordsSent(data.getRecordsSent());
            processing.withXmlContent(data.getXmlContent());
            processing.withXmlFileName(data.getXmlFileName());
            Boolean result = (Boolean) mockingDetails(contributionClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
            processing.withXmlFileResult(result);
            return result;
        }).when(contributionClientSpy).updateContributions(any());

        // Create at least 3 ACTIVE concor_contribution rows for this test to be meaningful:
        var updatedIds = testDataClient.updateConcorContributionStatus(UpdateConcorContributionStatusRequest.builder()
                .status(ConcorContributionStatus.ACTIVE).recordCount(3).build());
        processing.withUpdatedIds(updatedIds);

        // Call the processDailyFiles() method under test (date range in case executing near to midnight)
        var startDate = LocalDate.now();
        contributionService.processDailyFiles();
        var endDate = LocalDate.now();

        // Fetch a couple of items of information from the maat-api to use during validation:
        updatedIds.forEach(id -> processing.withConcurContribution(testDataClient.getConcorContribution(id)));
        contributionClientSpy.findContributionFiles(startDate, endDate).stream() // retrieve the contribution_file row's content
                .filter(content -> content.contains("<filename>" + processing.xmlFileName() + "</filename>"))
                .findFirst().ifPresentOrElse(processing::withContributionFileContent,
                        () -> softly.fail("no contribution_file named `" + processing.xmlFileName() + "` was found"));
        var processed = processing.build(); // it's solely validation from now on

        softly.assertThat(processed.updatedIds()).hasSize(3);
        softly.assertThat(processed.activeIds()).containsAll(processed.updatedIds());
        softly.assertThat(processed.sentIds()).containsAll(processed.updatedIds());

        softly.assertThat(processed.recordsSent()).isGreaterThanOrEqualTo(3);
        softly.assertThat(processed.xmlCcIds()).containsAll(processed.updatedIds());
        processed.concurContributions().forEach(concorContribution ->
                softly.assertThat(processed.xmlContent()).contains("<maat_id>" + concorContribution.getRepId() + "</maat_id>"));
        softly.assertThat(processed.xmlFileName()).isNotBlank();
        softly.assertThat(processed.xmlFileResult()).isEqualTo(Boolean.TRUE);

        checkConcorContributionsAreSent(processed.concurContributions(), startDate, endDate);

        checkContributionFileIsCreated(processed.contributionFileContent(), startDate, endDate);
        processed.concurContributions().forEach(concorContribution ->
                softly.assertThat(processed.contributionFileContent()).contains("<maat_id>" + concorContribution.getRepId() + "</maat_id>"));
    }

    private void checkConcorContributionsAreSent(final List<ConcorContributionResponseDTO> concorContributions, final LocalDate startDate, final LocalDate endDate) {
        concorContributions.forEach(concorContribution -> {
            softly.assertThat(concorContribution.getStatus()).isEqualTo(ConcorContributionStatus.SENT);
            softly.assertThat(concorContribution.getContribFileId()).isNotNull();
            softly.assertThat(concorContribution.getUserModified()).isEqualTo("TOGDATA"); // actually we should be checking for "DCES"
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
}
