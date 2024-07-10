package uk.gov.justice.laa.crime.dces.integration.model.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class UpdateFdcContributionRequest {
  private Integer fdcContributionId;
  private Integer repId;
  private String previousStatus;
  private FdcContributionsStatus newStatus;
}