package uk.gov.justice.laa.crime.dces.integration.datasource.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * An entity class represents a table in a relational database
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Builder
@Table(name = "case_migration")
public class CaseMigrationEntity {

    @Id
    private Long maatId;
    private String recordType;
    @Id
    private Long concorContributionId;
    @Id
    private Long fdcId;
    private Long batchId;
    private boolean isProcessed;
    private LocalDateTime processedDate;
    private Integer httpStatus;
    private String payload;

}