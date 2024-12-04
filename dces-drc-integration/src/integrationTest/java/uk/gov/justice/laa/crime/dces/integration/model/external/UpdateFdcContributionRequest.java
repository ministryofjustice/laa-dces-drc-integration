package uk.gov.justice.laa.crime.dces.integration.model.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus;

@Data
@AllArgsConstructor
public class UpdateFdcContributionRequest {
  private Long fdcContributionId;
  private Long repId;
  private String previousStatus;
  private FdcContributionsStatus newStatus;
}