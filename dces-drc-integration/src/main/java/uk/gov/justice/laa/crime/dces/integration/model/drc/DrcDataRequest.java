package uk.gov.justice.laa.crime.dces.integration.model.drc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrcDataRequest {

    private Integer contributionId;

}