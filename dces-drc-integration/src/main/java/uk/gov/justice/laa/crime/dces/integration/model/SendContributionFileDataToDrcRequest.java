package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class SendContributionFileDataToDrcRequest {
    private final CONTRIBUTIONS data;
    private final Map<String, String> meta = new HashMap<>();
}
