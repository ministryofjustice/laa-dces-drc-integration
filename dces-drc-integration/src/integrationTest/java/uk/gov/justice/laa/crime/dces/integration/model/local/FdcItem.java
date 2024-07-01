package uk.gov.justice.laa.crime.dces.integration.model.local;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FdcItem {
  private int fdcId;
  private FdcItemType itemType;
  private String adjustmentReason;
  private String paidAsClaimed;
  private String latestCostIndicator;
  private String userCreated;
}
