package uk.gov.justice.laa.crime.dces.integration.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsStatus;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateFdcContributionRequest {
  @Schema(description = "The REP ID when creating FdcContribution", example = "123")
  @NotNull
  private long repId;

  @Schema(description = "LGFS completion status", example = "Y", allowableValues = "Y,N")
  @Size(max = 1)
  private String lgfsComplete;

  @Size(max = 1)
  private String agfsComplete;

  @Size(max = 1)
  private String manualAcceleration;

  private FdcContributionsStatus status;
}
