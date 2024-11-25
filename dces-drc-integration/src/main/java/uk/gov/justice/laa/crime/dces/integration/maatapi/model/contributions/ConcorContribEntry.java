package uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "concorContributionId",
        "xmlContent"
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConcorContribEntry {
    private Long concorContributionId;
    private String xmlContent;
}
