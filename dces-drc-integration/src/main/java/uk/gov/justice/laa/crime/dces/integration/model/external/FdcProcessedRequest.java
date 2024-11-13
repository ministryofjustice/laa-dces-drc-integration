package uk.gov.justice.laa.crime.dces.integration.model.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FdcProcessedRequest {
    private final Integer fdcId;
    private final String errorText;
}