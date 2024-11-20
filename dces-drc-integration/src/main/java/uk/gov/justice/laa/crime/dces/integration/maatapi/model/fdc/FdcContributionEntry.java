package uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FdcContributionEntry {
    private Long maatId;
    private Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate sentenceOrderDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateCalculated;
    private BigDecimal finalCost;
    private BigDecimal lgfsCost;
    private BigDecimal agfsCost;
    private String userModified;
    private String userCreated;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateCreated;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateModified;
    private String accelerate;
    private BigDecimal judApportionPercent;
    private BigDecimal agfsVat;
    private Long contFileId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateReplaced;
    private FdcContributionsStatus status;
    private String lgfsComplete;
    private String agfsComplete;
    private BigDecimal vat;
    private BigDecimal lgfsVat;}
