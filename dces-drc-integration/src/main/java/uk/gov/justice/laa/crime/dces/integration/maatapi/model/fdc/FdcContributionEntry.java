package uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "fdcContributions"
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FdcContributionEntry {
    private Integer id;
    private Integer maatId;
    private LocalDate sentenceOrderDate;
    private LocalDate dateCalculated;
    private BigDecimal finalCost;
    private BigDecimal lgfsCost;
    private BigDecimal agfsCost;
}
