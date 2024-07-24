package uk.gov.justice.laa.crime.dces.integration.model.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.crime.dces.integration.model.local.FdcItemType;
import uk.gov.justice.laa.crime.dces.integration.utils.JsonLocalDateDeserializer;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FdcItem {
  private int id;
  private int fdcId;
  private FdcItemType itemType;
  private String adjustmentReason;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  @JsonDeserialize(using= JsonLocalDateDeserializer.class)
  private LocalDateTime dateCreated;
  private String paidAsClaimed;
  private String latestCostInd;
  private String userCreated;
}
