package uk.gov.justice.laa.crime.dces.integration.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.exception.ErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/dces/v1")
public class AckFromDrcController {

    @Autowired
    private FdcService fdcService;

    @Autowired
    private ContributionService contributionService;

    @Timed(value = "laa_dces_drc_service_process_drc_update_fdc",
            description = "Time taken to process the updates for FDC from DRC and passing this for downstream processing.")
    @PostMapping(value = "/fdc")
    @Operation(description = "Processing the updates for FDC from DRC and passing this for downstream processing.")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400",
            description = "Bad request.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500",
            description = "Server Error.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public void fdc(@NotNull @RequestBody final FdcAckFromDrc fdcAckFromDrc) {
        log.info("Received FDC acknowledgement from DRC {}", fdcAckFromDrc);
        UpdateLogFdcRequest updateLogFdcRequest = UpdateLogFdcRequest.builder()
                .fdcId(fdcAckFromDrc.data().fdcId())
                .errorText(fdcAckFromDrc.data().errorText())
                .build();
        fdcService.processFdcUpdate(updateLogFdcRequest);
    }

    @Timed(value = "laa_dces_drc_service_process_drc_update_contributions",
            description = "Time taken to process the updates for concorContribution from DRC and passing this for downstream processing.")
    @PostMapping(value = "/contribution")
    @Operation(description = "Processing the updates for concorContribution from DRC and passing this for downstream processing.")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400",
            description = "Bad request.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500",
            description = "Server Error.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public void concorContribution(@NotNull @RequestBody final ConcorContributionAckFromDrc concorContributionAckFromDrc) {
        log.info("Received concorContribution acknowledgement from DRC {}", concorContributionAckFromDrc);
        UpdateLogContributionRequest updateLogContributionRequest = UpdateLogContributionRequest.builder()
                .concorId(concorContributionAckFromDrc.data().concorContributionId())
                .errorText(concorContributionAckFromDrc.data().errorText())
                .build();
        contributionService.processContributionUpdate(updateLogContributionRequest);
    }
}