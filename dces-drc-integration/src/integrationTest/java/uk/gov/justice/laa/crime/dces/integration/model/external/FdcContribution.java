package uk.gov.justice.laa.crime.dces.integration.model.external;

import jakarta.xml.bind.annotation.XmlType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus;

@Data
@Builder
@XmlType
public class FdcContribution {

  private Integer id;
  private Integer maatId;
  private LocalDate sentenceOrderDate;
  private LocalDate dateCalculated;
  private BigDecimal finalCost;
  private BigDecimal lgfsCost;
  private BigDecimal agfsCost;
  private String userModified;
  private String userCreated;
  private LocalDate dateCreated;
  private LocalDate dateModified;
  private String accelerate;
  private BigDecimal judApportionPercent;
  private BigDecimal agfsVat;
  private Integer contFileId;
  private LocalDate dateReplaced;
  private FdcContributionsStatus status;
  private String lgfsComplete;
  private String agfsComplete;
  private BigDecimal vat;
  private BigDecimal lgfsVat;
}