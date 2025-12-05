package uk.gov.justice.laa.crime.dces.integration.controller;

import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionAckService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcAckService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/dces/v1")
public class AckFromDrcController {

    @Autowired
    private FdcAckService fdcAckService;

    @Autowired
    private ContributionAckService contributionAckService;

    @Observed(name = "UpdateFromDrcAPI.fdc", contextualName = "Process Updates for FDC", lowCardinalityKeyValues = {"priority", "high"})
    @PostMapping(value = "/fdc")
    @Operation(description = "Processing the updates for FDC from DRC and passing this for downstream processing.")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404",
        description = "Not Found.",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class)))
    public void fdc(@NotNull @RequestBody final FdcAckFromDrc fdcAckFromDrc) {
        log.info("Received FDC acknowledgement from DRC {}", fdcAckFromDrc);
        fdcAckService.handleFdcProcessedAck(fdcAckFromDrc);
    }

    @Observed(name = "UpdateFromDrcAPI.contribution", contextualName = "Process Updates for Contribution", lowCardinalityKeyValues = {"priority", "high"})
    @PostMapping(value = "/contribution")
    @Operation(description = "Processing the updates for concorContribution from DRC and passing this for downstream processing.")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404",
        description = "Not Found.",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class)))
    public void concorContribution(@NotNull @RequestBody final ConcorContributionAckFromDrc concorContributionAckFromDrc) {
        log.info("Received concorContribution acknowledgement from DRC {}", concorContributionAckFromDrc);
        contributionAckService.handleContributionProcessedAck(concorContributionAckFromDrc);
    }
}