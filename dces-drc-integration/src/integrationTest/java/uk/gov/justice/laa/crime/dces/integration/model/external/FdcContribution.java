package uk.gov.justice.laa.crime.dces.integration.model.external;

import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@XmlType
public class FdcContribution {

  private Integer id;

  private Integer contFileId;

  private LocalDate dateCalculated;

  private LocalDate dateReplaced;

  private FdcContributionsStatus status;

  @Size(max = 1)
  private String lgfsComplete;

  @Size(max = 1)
  private String agfsComplete;

  @Builder.Default
  private BigDecimal finalCost = BigDecimal.valueOf(0);

  @Builder.Default
  private BigDecimal vat = BigDecimal.valueOf(0);

  @Builder.Default
  private BigDecimal lgfsCost = BigDecimal.valueOf(0);

  @Builder.Default
  private BigDecimal agfsCost = BigDecimal.valueOf(0);

  @Builder.Default
  private BigDecimal lgfsVat = BigDecimal.valueOf(0);
  @Builder.Default
  private BigDecimal agfsVat = BigDecimal.valueOf(0);
  @Builder.Default
  private BigDecimal judApportionPercent = BigDecimal.valueOf(0);

  private String accelerate;

  private LocalDate dateModified;

  private LocalDate dateCreated;

  @Builder.Default
  private String userCreated = "DCES";

  private String userModified;
}