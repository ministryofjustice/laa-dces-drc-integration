package uk.gov.justice.laa.crime.dces.integration.model.local;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FdcContribution {
  private int repOrderId;
  private String lgfsComplete;
  private String agfsComplete;
  private String status;
}
