package uk.gov.justice.laa.crime.dces.integration.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.EventTypeRepository;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final CaseSubmissionRepository caseSubmissionRepository;
    private final EventTypeRepository eventTypeRepository;

    public List<CaseSubmissionEntity> getAllCaseSubmissions(){
        return caseSubmissionRepository.findAll();
    }

    public CaseSubmissionEntity saveEntity(CaseSubmissionEntity entity){
        return caseSubmissionRepository.save(entity);
    }

    public boolean logFdc(EventType eventType, Long batchId, Long traceId, Fdc fdcObject, HttpStatusCode httpStatusCode, String payload){
        // default fdcObject if null is passed. No ids is a valid scenario.
        fdcObject = Objects.requireNonNullElse(fdcObject, new Fdc());

        var entity = createCaseSubmissionEntity(eventType, batchId, traceId, fdcObject.getMaatId(), httpStatusCode, payload);
        entity.setRecordType(RecordType.FDC);
        entity.setFdcId(fdcObject.getId());
        saveEntity(entity);
        return true;
    }

    public boolean logFdc(EventType eventType, Long batchId, Fdc fdcObject, HttpStatusCode httpStatusCode, String payload){
        return logFdc(eventType, batchId, null, fdcObject, httpStatusCode, payload);
    }

    public boolean logConcor(Long concorContributionId, EventType eventType, Long batchId, Long traceId, CONTRIBUTIONS contributionsObject, HttpStatusCode httpStatusCode, String payload){
        // default fdcObject if null is passed. No ids is a valid scenario.
        contributionsObject = Objects.requireNonNullElse(contributionsObject, new CONTRIBUTIONS());

        var entity = createCaseSubmissionEntity(eventType, batchId, traceId, contributionsObject.getMaatId(), httpStatusCode, payload);
        entity.setRecordType(RecordType.CONTRIBUTION);
        entity.setConcorContributionId(concorContributionId);
        saveEntity(entity);
        return true;
    }

    public boolean logConcor(Long concorContributionId, EventType eventType, Long batchId, CONTRIBUTIONS contributionsObject, HttpStatusCode httpStatusCode, String payload){
        return logConcor(concorContributionId, eventType, batchId, null, contributionsObject, httpStatusCode, payload);
    }

    private CaseSubmissionEntity createCaseSubmissionEntity(EventType eventType, Long batchId, Long traceId, Long maatId, HttpStatusCode httpStatusCode, String payload){
        Integer httpStatus = (Objects.nonNull(httpStatusCode)) ? httpStatusCode.value() : null;
        var caseSubmissionEntity = CaseSubmissionEntity.builder()
                .batchId(batchId)
                .traceId(traceId)
                .maatId(maatId)
                .httpStatus(httpStatus)
                .payload(payload)
                .build();
        setEventType(eventType, caseSubmissionEntity);
        return caseSubmissionEntity;
    }

    public Long generateBatchId(){
        return caseSubmissionRepository.getNextBatchId();
    }
    public Long generateTraceId(){
        return caseSubmissionRepository.getNextTraceId();
    }

    private void setEventType(EventType type, CaseSubmissionEntity entity) {
        if (Objects.nonNull(type)) {
            EventTypeEntity typeEntity = eventTypeRepository.getEventTypeEntityByDescriptionEquals(type.getName());
            entity.setEventType(typeEntity.getId());
        }
        else{
            throw new DcesDrcServiceException("EventType cannot be null");
        }
    }
}
