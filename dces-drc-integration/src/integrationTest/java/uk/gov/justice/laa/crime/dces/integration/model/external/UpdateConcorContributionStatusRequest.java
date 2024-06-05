package uk.gov.justice.laa.crime.dces.integration.model.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateConcorContributionStatusRequest {
    private ConcorContributionStatus status;
    private int recordCount;
}
