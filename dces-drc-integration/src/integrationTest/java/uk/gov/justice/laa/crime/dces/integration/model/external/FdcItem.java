package uk.gov.justice.laa.crime.dces.integration.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItemType;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FdcItem {
  private int fdcId;
  private FdcItemType itemType;
  private String adjustmentReason;
  private LocalDateTime dateCreated;
  private String paidAsClaimed;
  private String latestCostInd;
  private String userCreated;
}
