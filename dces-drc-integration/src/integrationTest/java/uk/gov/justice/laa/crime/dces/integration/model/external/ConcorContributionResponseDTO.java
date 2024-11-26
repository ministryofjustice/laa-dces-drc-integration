package uk.gov.justice.laa.crime.dces.integration.model.external;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcorContributionResponseDTO {

    @NotBlank
    private Long id;
    private Long repId;
    private LocalDate dateCreated;
    private String userCreated;
    private LocalDate dateModified;
    private String userModified;
    private Long seHistoryId;
    @NotBlank
    private ConcorContributionStatus status;
    private Long contribFileId;
    private Long ackFileId;
    private String ackCode;
}