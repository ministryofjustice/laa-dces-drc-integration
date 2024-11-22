package uk.gov.justice.laa.crime.dces.integration.testing;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.EventLogAssertService;
import uk.gov.justice.laa.crime.dces.integration.service.spy.ContributionProcessSpy;
import uk.gov.justice.laa.crime.dces.integration.service.spy.SpyFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnabledIf(expression = "#{environment['sentry.environment'] == 'development'}", loadContext = true)
@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContributionIntegrationTest {
    @InjectSoftAssertions
    private SoftAssertions softly;

    @SpyBean
    private EventService eventService;

    @SpyBean
    private CaseSubmissionRepository caseSubmissionRepository;

    @Autowired
    private SpyFactory spyFactory;

    @Autowired
    private ContributionService contributionService;

    @Autowired
    private EventLogAssertService eventLogAssertService;
    @Captor
    ArgumentCaptor<CaseSubmissionEntity> caseSubmissionEntityArgumentCaptor;

    private static final Long TEST_BATCH_ID = -333L;

    @AfterEach
    void assertAll() {
        eventLogAssertService.deleteAllByBatchId(TEST_BATCH_ID);
        softly.assertAll();
    }

    @BeforeEach
    public void setBatchIdToTest(){
        eventLogAssertService.deleteAllByBatchId(TEST_BATCH_ID);
        when(eventService.generateBatchId()).thenReturn(TEST_BATCH_ID);
    }
    @BeforeAll
    public void setupHelper(){
        eventLogAssertService.setBatchId(TEST_BATCH_ID);
        eventLogAssertService.setSoftly(softly);
    }

    @Test
    void testProcessContributionUpdateWhenNotFound() {
        final String errorText = "The request has failed to process";
        final var contributionProcessedRequest = ContributionProcessedRequest.builder()
                .concorId(9L)
                .errorText(errorText)
                .build();
        softly.assertThatThrownBy(() -> contributionService.handleContributionProcessedAck(contributionProcessedRequest))
                .isInstanceOf(WebClientResponseException.class);
        assertProcessConcorCaseSubmissionCreation(contributionProcessedRequest, HttpStatus.NOT_FOUND);
    }

    @Test
    void testProcessContributionUpdateWhenFound() {
        final String errorText = "Error Text updated successfully.";
        final var contributionProcessedRequest = ContributionProcessedRequest.builder()
                .concorId(47959912L)
                .errorText(errorText)
                .build();
        final Long response = contributionService.handleContributionProcessedAck(contributionProcessedRequest);
        softly.assertThat(response).isPositive();
        assertProcessConcorCaseSubmissionCreation(contributionProcessedRequest, HttpStatus.OK);
    }

    // Just verify we're submitting what is expected to the DB. Persistence testing itself is done elsewhere.
    private void assertProcessConcorCaseSubmissionCreation(ContributionProcessedRequest request, HttpStatusCode expectedStatusCode) {
        CaseSubmissionEntity expectedCaseSubmission = CaseSubmissionEntity.builder()
                .concorContributionId(request.getConcorId())
                .payload(request.getErrorText())
                .eventType(eventLogAssertService.getIdForEventType(EventType.DRC_ASYNC_RESPONSE))
                .httpStatus(expectedStatusCode.value())
                .recordType(RecordType.CONTRIBUTION.getName())
                .processedDate(LocalDateTime.now())
                .build();
        verify(caseSubmissionRepository).save(caseSubmissionEntityArgumentCaptor.capture());
        CaseSubmissionEntity actualCaseSubmission = caseSubmissionEntityArgumentCaptor.getValue();
        eventLogAssertService.assertCaseSubmissionsEqual(actualCaseSubmission, expectedCaseSubmission);
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

        final ContributionProcessSpy.ContributionProcessSpyBuilder watching = spyFactory.newContributionProcessSpyBuilder()
                .traceAndFilterGetContributionsActive(updatedIds)
                .traceAndStubSendContributionUpdate(id -> Boolean.TRUE)
                .traceUpdateContributions();

        // Call the processDailyFiles() method under test:
        final var startDate = LocalDate.now();
        contributionService.processDailyFiles();
        final var endDate = LocalDate.now();

        final ContributionProcessSpy watched = watching.build();

        // Fetch some items of information from the maat-api to use during validation:
        final var concorContributions = updatedIds.stream().map(spyFactory::getConcorContribution).toList();
        final long contributionFileId = watched.getXmlFileResult();
        final var contributionFile = spyFactory.getContributionsFile(contributionFileId);

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
        softly.assertThat(contributionFile.getDateModified()).isBetween(startDate, endDate);
        softly.assertThat(contributionFile.getUserModified()).isEqualTo("DCES");
        softly.assertThat(contributionFile.getDateSent()).isBetween(startDate, endDate);
        concorContributions.forEach(concorContribution ->
                softly.assertThat(contributionFile.getXmlContent()).contains("<maat_id>" + concorContribution.getRepId() + "</maat_id>"));

        eventLogAssertService.assertConcorEventLogging(12, 3,4,3,5);
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

        final ContributionProcessSpy.ContributionProcessSpyBuilder watching = spyFactory.newContributionProcessSpyBuilder()
                .traceAndFilterGetContributionsActive(updatedIds)
                .traceAndStubSendContributionUpdate(id -> Boolean.TRUE)
                .traceUpdateContributions();

        // Call the processDailyFiles() method under test:
        contributionService.processDailyFiles();

        final ContributionProcessSpy watched = watching.build();

        // Fetch some items of information from the maat-api to use during validation:
        final var concorContributions = updatedIds.stream().map(spyFactory::getConcorContribution).toList();

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
        eventLogAssertService.assertConcorEventLogging(1, 1,1,0,0);
    }

    /**
     * <h4>Scenario:</h4>
     * <p>A negative integration test which checks that when a concor_contribution fails to be sent to the DRC, the
     *    status is not updated to SENT (remains ACTIVE).</p>
     * <h4>Given:</h4>
     * <p>* Update 2 concor_contribution records to the ACTIVE status for the purposes of the test.</p>
     * <h4>When:</h4>
     * <p>* The {@link ContributionService#processDailyFiles()} method is called.</p>
     * <p>* However, sending the records to the DRC should fail.</p>
     * <h4>Then:</h4>
     * <p>1. The IDs of the 2 updated records are returned (by call to retrieve active contributions).</p>
     * <p>2. The integration's steps are called correctly to create a contribution file, which is persisted.</p>
     * <p>3. The updated IDs are included in the set of payloads sent to the DRC.</p>
     * <p>4. A contribution file gets created, with at least 2 failed records.<br>
     *    The generated XML content does not include either of the 2 linked rep_order IDs in a &lt;maat_id&gt; tag.</p>
     * <p>5. After the `processDailyFiles` method call returns, the concor_contribution entities corresponding to each
     *    of the updated IDs is checked:<br>
     *    - Each remains at status ACTIVE</p>
     *
     * Note: We actually update 3 records to ACTIVE, so that one can succeed (because no contribution file is created
     * at all if zero records are successfully sent to the DRC).
     *
     * @see <a href="https://dsdmoj.atlassian.net/browse/DCES-352">DCES-352</a> for test specification.
     */
    @Test
    void givenSomeActiveConcorContributions_whenProcessDailyFilesFailsToSend_thenTheirStatusIsNotUpdated() {
        // Update at least 3 concor_contribution rows to ACTIVE:
        final var updatedIds = spyFactory.updateConcorContributionStatus(ConcorContributionStatus.ACTIVE, 3);

        final ContributionProcessSpy.ContributionProcessSpyBuilder watching = spyFactory.newContributionProcessSpyBuilder()
                .traceAndFilterGetContributionsActive(updatedIds)
                .traceAndStubSendContributionUpdate(id -> !((updatedIds.get(0).equals(id)) || (updatedIds.get(1).equals(id)))) // first two fail
                .traceUpdateContributions();

        // Call the processDailyFiles() method under test:
        contributionService.processDailyFiles();

        final ContributionProcessSpy watched = watching.build();

        // Fetch some items of information from the maat-api to use during validation:
        final var concorContributions = updatedIds.stream().map(spyFactory::getConcorContribution).toList();
        final long contributionFileId = watched.getXmlFileResult();
        final var contributionFile = spyFactory.getContributionsFile(contributionFileId);

        softly.assertThat(updatedIds).hasSize(3).doesNotContainNull(); // 1.
        softly.assertThat(watched.getActiveIds()).containsAll(updatedIds); // 2.
        softly.assertThat(watched.getSentIds()).containsAll(updatedIds); // 3.

        softly.assertThat(watched.getXmlCcIds()).doesNotContain(updatedIds.get(0), updatedIds.get(1)); // 4.
        // If the failed concor_contribution rows have the same rep_order as the successful one, then skip these checks:
        if (!Objects.equals(concorContributions.get(0).getRepId(), concorContributions.get(2).getRepId())) {
            softly.assertThat(contributionFile.getXmlContent()).doesNotContain("<maat_id>" + concorContributions.get(0).getRepId() + "</maat_id>");
        }
        if (!Objects.equals(concorContributions.get(1).getRepId(), concorContributions.get(2).getRepId())) {
            softly.assertThat(contributionFile.getXmlContent()).doesNotContain("<maat_id>" + concorContributions.get(1).getRepId() + "</maat_id>");
        }
        softly.assertThat(concorContributions.get(0).getStatus()).isEqualTo(ConcorContributionStatus.ACTIVE); // 5.
        softly.assertThat(concorContributions.get(1).getStatus()).isEqualTo(ConcorContributionStatus.ACTIVE);


        Map<EventType, List<CaseSubmissionEntity>> savedEventsByType = eventLogAssertService.getEventTypeListMap(10);
        softly.assertThat(savedEventsByType.size()).isEqualTo(3); // Fdc should save 4 types of EventTypes.
        eventLogAssertService.assertEventNumberStatus(savedEventsByType.get(EventType.FETCHED_FROM_MAAT), 4, HttpStatus.OK, true);
        eventLogAssertService.assertEventNumberStatus(savedEventsByType.get(EventType.SENT_TO_DRC), 1, HttpStatus.OK, false); // 1 sends successfully.
        eventLogAssertService.assertEventNumberStatus(savedEventsByType.get(EventType.SENT_TO_DRC), 2, HttpStatus.BAD_REQUEST, false); // 2 send fails
        eventLogAssertService.assertEventNumberStatus(savedEventsByType.get(EventType.UPDATED_IN_MAAT), 2, HttpStatus.OK, false); // it should update successfully.
        eventLogAssertService.assertEventNumberStatus(savedEventsByType.get(EventType.UPDATED_IN_MAAT), 1, HttpStatus.INTERNAL_SERVER_ERROR, false); // this is the single 500 which tracks there were failures.
    }
}
