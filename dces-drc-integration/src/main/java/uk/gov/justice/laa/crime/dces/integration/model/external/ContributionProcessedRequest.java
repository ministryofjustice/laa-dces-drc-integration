package uk.gov.justice.laa.crime.dces.integration.model.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContributionProcessedRequest {
    private final Long concorId;
    private final String errorText;
}