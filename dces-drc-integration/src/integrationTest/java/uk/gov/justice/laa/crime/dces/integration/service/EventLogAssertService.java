package uk.gov.justice.laa.crime.dces.integration.service;

import lombok.Setter;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.EventTypeRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Setter
public class EventLogAssertService {


    @Autowired
    private CaseSubmissionRepository caseSubmissionRepository;
    @Autowired
    private EventTypeRepository eventTypeRepository;
    private Long batchId;
    private SoftAssertions softly;

    public void deleteAllByBatchId(Long batchId){
        caseSubmissionRepository.deleteAllByBatchId(batchId);
    }

    // helper methods.
    @NotNull
    public Map<EventType, List<CaseSubmissionEntity>> getEventTypeListMap(int numberOfAsserts) {
        List<CaseSubmissionEntity> savedEventsList = caseSubmissionRepository.findAllByBatchId(batchId);
        // for 3 fdcs, we should end up with 13 entries. So we need to verify what number of asserts we're doing.
        softly.assertThat(savedEventsList.size()).isEqualTo(numberOfAsserts);
        return getMapOfLoggedEvents(savedEventsList);
    }

    public void assertFdcEventLogging(int totalExpected, int numTypesExpected, int numGlobalUpdate, int numFetchedFromMaat, int numSentToDrc, int numUpdatedInMaat) {
        Map<EventType, List<CaseSubmissionEntity>> savedEventsByType = getEventTypeListMap(totalExpected);
        // check all successful
        assertEventBasics(savedEventsByType, numTypesExpected, numFetchedFromMaat, numSentToDrc, numUpdatedInMaat); // Fdc should save 4 types of EventTypes.
        assertEventNumberStatus(savedEventsByType.get(EventType.FDC_GLOBAL_UPDATE), numGlobalUpdate, HttpStatus.OK, true);
    }

    public void assertConcorEventLogging(int totalExpected, int numTypesExpected, int numFetchedFromMaat, int numSentToDrc, int numUpdatedInMaat) {
        Map<EventType, List<CaseSubmissionEntity>> savedEventsByType = getEventTypeListMap(totalExpected);
        // check all successful
        assertEventBasics(savedEventsByType, numTypesExpected, numFetchedFromMaat, numSentToDrc, numUpdatedInMaat);
    }

    private void assertEventBasics(Map<EventType, List<CaseSubmissionEntity>> savedEventsByType, int numTypesExpected, int numFetchedFromMaat, int numSentToDrc, int numUpdatedInMaat){
        softly.assertThat(savedEventsByType.size()).isEqualTo(numTypesExpected); // Concor should save 3 types of EventTypes.
        assertEventNumberStatus(savedEventsByType.get(EventType.FETCHED_FROM_MAAT), numFetchedFromMaat, HttpStatus.OK, true);
        assertEventNumberStatus(savedEventsByType.get(EventType.SENT_TO_DRC), numSentToDrc, HttpStatus.OK, true);
        assertEventNumberStatus(savedEventsByType.get(EventType.UPDATED_IN_MAAT), numUpdatedInMaat, HttpStatus.OK, true);
    }

    public void assertEventNumberStatus(List<CaseSubmissionEntity> eventSubmissionList, int numExpected, HttpStatusCode httpStatus, boolean isOnlyExpected){
        if(Objects.isNull(eventSubmissionList)){
            softly.assertThat(numExpected).isEqualTo(0);
        }
        else {
            int foundEvents = (int) eventSubmissionList.stream().filter(x -> httpStatus.value() == x.getHttpStatus()).count();
            softly.assertThat(foundEvents).isEqualTo(numExpected);
            if (isOnlyExpected) {
                softly.assertThat(foundEvents).isEqualTo(eventSubmissionList.size());
            }
        }
    }

    public void assertCaseSubmissionsEqual(CaseSubmissionEntity caseSubmission, CaseSubmissionEntity expectedCaseSubmission){
        softly.assertThat(caseSubmission.getFdcId()).isEqualTo(expectedCaseSubmission.getFdcId());
        softly.assertThat(caseSubmission.getConcorContributionId()).isEqualTo(expectedCaseSubmission.getConcorContributionId());
        softly.assertThat(caseSubmission.getPayload()).isEqualTo(expectedCaseSubmission.getPayload());
        softly.assertThat(caseSubmission.getEventType()).isEqualTo(expectedCaseSubmission.getEventType());
        softly.assertThat(caseSubmission.getProcessedDate().toLocalDate().toString()).isEqualTo(expectedCaseSubmission.getProcessedDate().toLocalDate().toString());
        softly.assertThat(caseSubmission.getHttpStatus()).isEqualTo(expectedCaseSubmission.getHttpStatus());
        softly.assertThat(caseSubmission.getConcorContributionId()).isEqualTo(expectedCaseSubmission.getConcorContributionId());
        softly.assertThat(caseSubmission.getRecordType()).isEqualTo(expectedCaseSubmission.getRecordType());
        softly.assertThat(caseSubmission.getBatchId()).isEqualTo(expectedCaseSubmission.getBatchId());
        softly.assertThat(caseSubmission.getMaatId()).isEqualTo(expectedCaseSubmission.getMaatId());
        softly.assertThat(caseSubmission.getTraceId()).isEqualTo(expectedCaseSubmission.getTraceId());
    }


    private EventType getEventType(int eventTypeId){
        Optional<EventTypeEntity> eventEntity = eventTypeRepository.findById(eventTypeId);
        return Stream.of(EventType.values()).filter(x-> x.getName().equals(eventEntity.get().getDescription())).findFirst().get();
    }


    public int getIdForEventType(EventType eventType){
        EventTypeEntity eventEntity = eventTypeRepository.getEventTypeEntityByDescriptionEquals(eventType.getName());
        return eventEntity.getId();
    }

    private Map<EventType, List<CaseSubmissionEntity>> getMapOfLoggedEvents(List<CaseSubmissionEntity> eventSubmissionList) {
        return eventSubmissionList.stream().collect(Collectors.groupingBy(x-> getEventType(x.getEventType())));
    }
}
