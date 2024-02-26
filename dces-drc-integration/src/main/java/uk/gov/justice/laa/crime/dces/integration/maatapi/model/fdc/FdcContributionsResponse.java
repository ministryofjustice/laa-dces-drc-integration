package uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "fdcContributions"
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FdcContributionsResponse {
    private List<FdcContributionEntry> fdcContributions;
}
