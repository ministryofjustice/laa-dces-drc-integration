package uk.gov.justice.laa.crime.dces.integration.model.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendFdcFileDataToExternalRequest {
    private Integer fdcId;
}