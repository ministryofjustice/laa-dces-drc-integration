package uk.gov.justice.laa.crime.dces.integration.datasource.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * The result of how a Contribution or FDC was processed by the Debt Recovery Company (DRC).
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Builder
@Table(name = "drc_processing_status")
public class DrcProcessingStatusEntity {

    @Id
    @SequenceGenerator(name = "drc_processing_status_gen_seq", sequenceName = "drc_processing_status_gen_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "drc_processing_status_gen_seq")
    private Long id;
    private Long maatId;
    private Long concorContributionId;
    private Long fdcId;
    private String statusMessage;
    private String detail;
    @CreationTimestamp
    private LocalDateTime creationTimestamp;

}

