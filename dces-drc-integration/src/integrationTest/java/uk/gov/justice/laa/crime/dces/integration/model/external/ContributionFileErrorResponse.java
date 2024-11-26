package uk.gov.justice.laa.crime.dces.integration.model.external;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContributionFileErrorResponse {
    private final Long contributionFileId;
    private final Long contributionId;
    private final Long repId;
    private final String errorText;
    private final String fixAction;
    private final Long fdcContributionId;
    private final Long concorContributionId;
    private final LocalDateTime dateCreated;
}
