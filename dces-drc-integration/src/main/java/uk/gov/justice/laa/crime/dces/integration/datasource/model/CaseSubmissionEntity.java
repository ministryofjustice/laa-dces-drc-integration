package uk.gov.justice.laa.crime.dces.integration.datasource.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * An entity class represents a table in a relational database
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Builder
@Table(name = "case_submission")
public class CaseSubmissionEntity {

    @Id
    @SequenceGenerator(name = "case_submission_gen_seq", sequenceName = "case_submission_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "case_submission_gen_seq")
    private Long id;
    private Long batchId;
    private Long traceId;
    private Long maatId;
    private Long concorContributionId;
    private Long fdcId;
    private String recordType;
    @CreationTimestamp
    private LocalDateTime processedDate;
    private Integer eventType;
    private Integer httpStatus;
    private String payload;

    public void setRecordType(RecordType recordType){
        this.recordType = recordType.getName();
    }

}