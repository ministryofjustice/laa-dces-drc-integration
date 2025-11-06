package uk.gov.justice.laa.crime.dces.integration.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionErrorEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionErrorRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.EventTypeRepository;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static uk.gov.justice.laa.crime.dces.integration.utils.CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final CaseSubmissionRepository caseSubmissionRepository;
    private final CaseSubmissionErrorRepository caseSubmissionErrorRepository;
    private final EventTypeRepository eventTypeRepository;

    // get History Duration for use in clearing down history.
    private final String historyCutoffDays = System.getenv("SPRING_DATASOURCE_KEEPHISTORYDAYS");

    public List<CaseSubmissionEntity> getAllCaseSubmissions(){
        return caseSubmissionRepository.findAll();
    }

    public CaseSubmissionEntity saveEntity(CaseSubmissionEntity entity){
        return caseSubmissionRepository.save(entity);
    }

    public Long generateBatchId(){
        return caseSubmissionRepository.getNextBatchId();
    }
    public Long generateTraceId(){
        return caseSubmissionRepository.getNextTraceId();
    }

    public Long countHistoricalCaseSubmissionEntries() {
        Integer cutoffDays = getCutoffDays();
        // delete all entries that are older than the cutoff days ago, from first thing today.
        if (Objects.nonNull(cutoffDays)) {
            return caseSubmissionRepository.countByProcessedDateBefore(getCutoffDate(cutoffDays));
        }
        return 0L;
    }

    public Integer deleteHistoricalCaseSubmissionEntries() {
        Integer cutoffDays = getCutoffDays();
        // delete all entries that are older than the cutoff days ago, from first thing today.
        if (Objects.nonNull(cutoffDays)) {
            return caseSubmissionRepository.deleteByProcessedDateBefore(getCutoffDate(cutoffDays));
        }
        return 0;
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

    public boolean logFdcError( FdcAckFromDrc fdcAckFromDrc){
        var entity = createCaseSubmissionErrorEntity(fdcAckFromDrc);
        saveCaseSubmissionErrorEntity(entity);
        log.info("saved fdc error entity: {}", entity);
        return true;
    }

    public boolean logConcorContributionError(ConcorContributionAckFromDrc concorContributionAckFromDrc){

        var entity = createCaseSubmissionErrorEntity(concorContributionAckFromDrc);
        saveCaseSubmissionErrorEntity(entity);
        log.info("saved concor contribution error entity: {}", entity);
        return true;
    }

    public CaseSubmissionErrorEntity saveCaseSubmissionErrorEntity(CaseSubmissionErrorEntity entity){
        return caseSubmissionErrorRepository.save(entity);
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

    private void setEventType(EventType type, CaseSubmissionEntity entity) {
        if (Objects.nonNull(type)) {
            EventTypeEntity typeEntity = eventTypeRepository.getEventTypeEntityByDescriptionEquals(type.getName());
            entity.setEventType(typeEntity.getId());
        }
        else{
            throw new DcesDrcServiceException("EventType cannot be null");
        }
    }

    protected Integer getCutoffDays(){
        if(Objects.isNull(historyCutoffDays)){
            log.error("No History Cutoff Days set in environment.");
        }
        try{
            return Integer.valueOf(historyCutoffDays);
        } catch (NumberFormatException e){
            log.error("History Cutoff Days incorrectly formatted.");
        }
        return null;
    }

    protected LocalDateTime getCutoffDate(Integer cutoff){
        return LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(1)
                .minusDays(cutoff);
    }

}
