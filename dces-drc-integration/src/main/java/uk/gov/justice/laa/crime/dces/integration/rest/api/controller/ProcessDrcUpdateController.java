package uk.gov.justice.laa.crime.dces.integration.rest.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.crime.dces.integration.model.drc.DrcDataRequest;
import uk.gov.justice.laa.crime.dces.integration.rest.common.ErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/internal/v1/dces-drc-integration")
public class ProcessDrcUpdateController {

    @Autowired
    private FdcService fdcService;

    @Autowired
    private ContributionService contributionService;

    @PostMapping(value = "/process-drc-update/fdc")
    @Operation(description = "Retrieve application details from crime apply datastore")
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
    public String processFdcUpdate(@NotEmpty @RequestBody final DrcDataRequest drcDataRequest) {
        log.info("Request received from DRC to update FDC {}", drcDataRequest);
        String response = fdcService.processFdcUpdate(drcDataRequest);
        log.info("Returning Contribution-FDC API Response as a: {}", response);
        return response;
    }

    @PostMapping(value = "/process-drc-update/contribution")
    @Operation(description = "Process Updates from DRC for Contributions.")
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
    public String processContributionUpdate(@NotEmpty @RequestBody final DrcDataRequest drcDataRequest) {
        log.info("Request received from DRC to update contribution {}", drcDataRequest);
        String response = contributionService.processContributionUpdate(drcDataRequest);
        log.info("Returning Contribution API Response as a: {}", response);
        return response;
    }
}