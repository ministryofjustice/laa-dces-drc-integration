package uk.gov.justice.laa.crime.dces.integration.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.RecordType;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.CaseSubmissionRepository;
import uk.gov.justice.laa.crime.dces.integration.datasource.repository.EventTypeRepository;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseSubmissionService {

    private final CaseSubmissionRepository caseSubmissionRepository;
    private final EventTypeRepository eventTypeRepository;

    public List<CaseSubmissionEntity> getAllCaseSubmissions(){
        return caseSubmissionRepository.findAll();
    }

    public CaseSubmissionEntity saveEntity(CaseSubmissionEntity entity){
        return caseSubmissionRepository.save(entity);
    }

    public boolean logFdcEvent(EventType eventType, BigInteger batchId, BigInteger traceId, Fdc fdcObject, Integer httpStatusCode, String payload){
        var entity = createCaseSubmissionEntity(eventType, batchId, traceId, fdcObject.getMaatId(), httpStatusCode, payload);
        entity.setRecordType(RecordType.FDC);
        entity.setFdcId(fdcObject.getId());
        saveEntity(entity);
        return true;
    }

    public boolean logContributionCall(EventType eventType, BigInteger batchId, BigInteger traceId, CONTRIBUTIONS contributionsObject, Integer httpStatusCode, String payload){
        var entity = createCaseSubmissionEntity(eventType, batchId, traceId, contributionsObject.getMaatId(), httpStatusCode, payload);
        entity.setRecordType(RecordType.CONTRIBUTION);
        entity.setConcorContributionId(contributionsObject.getId());
        saveEntity(entity);
        return true;
    }

    private CaseSubmissionEntity createCaseSubmissionEntity(EventType eventType, BigInteger batchId, BigInteger traceId, BigInteger maatId, Integer httpStatus, String payload){
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

    public BigInteger generateBatchId(){
        return caseSubmissionRepository.getNextBatchId();
    }
    public BigInteger generateTraceId(){
        return caseSubmissionRepository.getNextTraceId();
    }

    private void setEventType(EventType type, CaseSubmissionEntity entity){
        EventTypeEntity typeEntity = eventTypeRepository.getEventTypeEntityByDescriptionEquals(type.getName());
        entity.setEventType(typeEntity.getId());
    }
}
