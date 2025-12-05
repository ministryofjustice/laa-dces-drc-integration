package uk.gov.justice.laa.crime.dces.integration.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionFileService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;

/**
 * This is a simple temporary controller to handle some test endpoints.
 * This feature should not be enabled in production.
 */
@ConditionalOnProperty(prefix = "feature", name = "temp-test-endpoints")
@RequestMapping(TempTestController.PREFIX)
@RequiredArgsConstructor
@RestController
@Slf4j
public class TempTestController {
    static final String PREFIX = "/api/dces/test";
    private final StubValidation stubValidation;
    private final ObjectMapper objectMapper;
    private final DrcClient drcClient;
    private static final int REQUEST_ID_LIST_SIZE_LIMIT_CONCOR = 350;
    private static final int REQUEST_ID_LIST_SIZE_LIMIT_FDC = 1000;
    private final ContributionFileService concorContributionsService;
    private final FdcService fdcService;

    /**
     * Check we have connectivity with almost no side effects (just a log line).
     */
    @GetMapping(value = "")
    public String getTest() {
        log.info("Received GET {}", PREFIX);
        return "GET Test Successful";
    }

    /**
     * Forward a hard-coded fake FDC to the fdc endpoint at Advantis.
     */
    @GetMapping(value = "/fdc")
    public String forwardFakeFdc() {
        log.info("Received GET {}/fdc", PREFIX);
        FdcReqForDrc request = FdcReqForDrc.of(106, fakeFdcObj());
        try {
            log.info("Forwarding fake FDC JSON [{}]", objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            // do nothing.
        }
        drcClient.sendFdcReqToDrc(request);
        return "OK";
    }

    private static FdcFile.FdcList.Fdc fakeFdcObj() {
        FdcFile.FdcList.Fdc fdc = new FdcFile.FdcList.Fdc();
        fdc.setId(94L);
        fdc.setMaatId(105L);
        fdc.setSentenceDate(LocalDate.parse("2023-05-21"));
        fdc.setCalculationDate(null);
        fdc.setAgfsTotal(null);
        fdc.setLgfsTotal(BigDecimal.valueOf(2000.0));
        fdc.setFinalCost(BigDecimal.valueOf(3500.0));
        return fdc;
    }

    /**
     * Forward a provided FDC record to the fdc endpoint at Advantis.
     */
    @Operation(description = "<h3> ** Use with caution, only if you are certain !!! ** </h3>"
        + "*This operation is not part of normal processing*<br>"
        + "*It bypasses normal processing and can send incorrect/sensitive/test data to a third party*<br>"
        + "*Only to be used during testing or fault-finding*<br><br>"
        + "When given a FDC object as JSON request body, send it to the DRC")
    @PostMapping(value = "/fdc")
    public String forwardFdc(@RequestBody FdcReqForDrc request) {
        log.info("Received POST {}/fdc", PREFIX);
        try {
            log.info("Forwarding posted FDC JSON [{}]", objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            // do nothing.
        }
        drcClient.sendFdcReqToDrc(request);
        return "OK";
    }


    /**
     * Forward a provided Concor Contribution record to the contribution endpoint at Advantis.
     */
    @Operation(description = "<h3> ** Use with caution, only if you are certain !!! ** </h3>"
        + "*This operation is not part of normal processing*<br>"
        + "*It bypasses normal processing and can send incorrect/sensitive/test data to a third party*<br>"
        + "*Only to be used during testing or fault-finding*<br><br>"
        + "When given a Concor Contribution object as JSON request body, send it to the DRC")
    @PostMapping(value = "/contribution")
    public String forwardConcorContribution(@RequestBody ConcorContributionReqForDrc request) {
        log.info("Received POST {}/contribution", PREFIX);
        try {
            log.info("Forwarding posted Concor Contribution JSON [{}]", objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            // do nothing.
        }
        drcClient.sendConcorContributionReqToDrc(request);
        return "OK";
    }

    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @PostMapping(value = "/send-contributions")
    @Operation(description = "<h3> ** Use with caution, only if you are certain !!! ** </h3>"
        + "*This operation is not part of normal processing*<br>"
        + "*It bypasses normal processing and can send production data to a third party*<br>"
        + "<ul>"
        + "<li>Obtains XML for each concor contribution ID in the list.</li>"
        + "<li>Sends it to the DRC</li>"
        + "<li>Does NOT create a Contributions File for the sent concor contributions.</li>"
        + "<li>Does NOT update each successfully processed concor contribution to SENT in MAAT.</li>"
        + "</ul>"
        + "*Only to be used during testing or fault-finding*<br><br>"
        + "When given a list of Concor Contribution IDs, get the XML for each and send them to the DRC"
        + "Returns the list of Concor Contribution Entries (ID + XML) that were sent to DRC")

    public ResponseEntity<List<ConcorContribEntry>> sendConcorContributionXmlsToDRC(@RequestBody List<Long> idList) {

        log.info("Request received to get and send the XML for {} IDs", idList.size());
        if (idList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID List Empty");
        } else if (idList.size() > REQUEST_ID_LIST_SIZE_LIMIT_CONCOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many IDs provided, max is " + REQUEST_ID_LIST_SIZE_LIMIT_CONCOR);
        } else {
            return ResponseEntity.ok(concorContributionsService.sendContributionsToDrc(idList));
        }
    }

    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @PostMapping(value = "/send-fdcs")
    @Operation(description = "<h3> ** Use with caution, only if you are certain !!! ** </h3>"
        + "*This operation is not part of normal processing*<br>"
        + "*It bypasses normal processing and can send production data to a third party*<br>"
        + "<ul>"
        + "<li>Obtains details for each FDC contribution ID in the list.</li>"
        + "<li>Sends it to the DRC</li>"
        + "<li>Does NOT create a Contributions File for the sent FDC contributions.</li>"
        + "<li>Does NOT update each successfully processed FDC contribution to SENT in MAAT.</li>"
        + "</ul>"
        + "*Only to be used during testing or fault-finding*<br><br>"
        + "When given a list of FDC Contribution IDs, get the details for each and send them to the DRC"
        + "Returns the list of FDC records that were sent to DRC")
    public ResponseEntity<List<Fdc>> sendFdcContributionsToDRC(@RequestBody List<Long> idList) {

        log.info("Request received to get and send the details for {} FDC IDs", idList.size());
        if (idList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID List Empty");
        } else if (idList.size() > REQUEST_ID_LIST_SIZE_LIMIT_FDC) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many IDs provided, max is " + REQUEST_ID_LIST_SIZE_LIMIT_FDC);
        } else {
            return ResponseEntity.ok(fdcService.sendFdcsToDrc(idList));
        }
    }

    /**
     * Throw one of a number of different exception types so the caller can check how it is reported to API consumers.
     * You csn also use a pseudoStatus out of range (e.g. 504) to check validation of controller method arguments.
     * These should probably be unit tests to check that each response is an appropriate-looking ProblemDetail.
     */
    @GetMapping(value = "/error/{pseudoStatus}")
    public String getTestError(@PathVariable("pseudoStatus") @Min(400) @Max(503) int pseudoStatus) {
        log.info("Received GET {}/error/{}", PREFIX, pseudoStatus);
        switch (pseudoStatus) {
            case 400 -> throw new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
            case 401 -> throw new WebClientResponseException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), null, null, null);
            case 403 -> throw new WebClientResponseException(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase(), null, null, null);
            case 404 -> throw WebClientResponseException.create(pseudoStatus, "Message for not-found-404", null, null, null);
            case 416 -> stubValidation.notNull_notEmpty(null, null);
            case 417 -> stubValidation.validSelf(stubValidation);
            case 418 -> throw new WebClientResponseException(HttpStatus.I_AM_A_TEAPOT.value(), HttpStatus.I_AM_A_TEAPOT.getReasonPhrase(), null, null, null);
            case 500 -> throw new NullPointerException("Message for null-pointer-exception-500");
            case 501 -> throw new UnsupportedOperationException("Message for unsupported-operation-exception-501");
            case 503 -> throw new WebClientResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(), HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(), null, null, null);
            default -> throw new IllegalArgumentException("Message for illegal-argument-exception-" + pseudoStatus + " (try 400,401,403,404,416,417,418,500,501,503)");
        }
        return "OK";
    }

    /**
     * This nested class is solely used to generate validation exceptions for the test /api/dces/test/error/{416,417}
     * endpoints. It's a `@Service` so that Spring wraps it in a dynamic proxy to do jakarta-validation validation.
     */
    @SuppressWarnings({"FieldCanBeLocal", "SameParameterValue", "unused"})
    @Service
    @Validated
    static class StubValidation {
        @NotNull
        private final String member1 = null;
        @NotEmpty
        private final String member2 = null;
        void notNull_notEmpty(@NotNull final String param1, @NotEmpty final String param2) {
            // do nothing.
        }
        void validSelf(@Valid final StubValidation self) {
            // do nothing.
        }
    }
}
