package uk.gov.justice.laa.crime.dces.integration.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;

import java.util.Random;

/**
 * This is a Stub to simulate the request and response from a 3rd Party (DRC).
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/dces/v1-stub")
@SuppressWarnings("squid:S2245") // Safe to use random here, as it's not being used in any fashion beyond a return value.
public class StubAckFromDrcController {
    private static final Random random = new Random();
    private final StubValidation stubValidation;

    public static int getRandomZeroOrOne() {
        return random.nextInt(2);
    }

    @PostMapping(value = "/fdc")
    public void fdc(@NotNull @RequestBody final FdcAckFromDrc fdcAckFromDrc) {
        log.info("Received FDC acknowledgement from DRC {}", fdcAckFromDrc);
        if (getRandomZeroOrOne() != 0) {
            throw new MaatApiClientException(HttpStatus.BAD_REQUEST, "Bad Request");
        }
    }

    @PostMapping(value = "/contribution")
    public void contribution(@NotNull @RequestBody final ContributionAckFromDrc contributionAckFromDrc) {
        log.info("Received contribution acknowledgement from DRC {}", contributionAckFromDrc);
        if (getRandomZeroOrOne() != 0) {
            throw new MaatApiClientException(HttpStatus.BAD_REQUEST, "Bad Request");
        }
    }

    @GetMapping(value = "/test")
    public String getTest() {
        log.info("Received GET test");
        return "GET Test Successful";
    }

    @GetMapping(value = "/test/{pseudoStatus}")
    public String getTestException(@PathVariable("pseudoStatus") @Min(400) @Max(503) int pseudoStatus) {
        log.info("Received GET test with pseudoStatus {}", pseudoStatus);
        switch (pseudoStatus) {
            case 400 -> throw new MaatApiClientException(HttpStatus.BAD_REQUEST, "Message for bad-request-400");
            case 401 -> throw new MaatApiClientException(HttpStatus.UNAUTHORIZED, "Message for unauthorized-401");
            case 403 -> throw new MaatApiClientException(HttpStatus.FORBIDDEN, "Message for forbidden-403");
            case 404 -> throw WebClientResponseException.create(pseudoStatus, "Message for not-found-404", null, null, null);
            case 416 -> stubValidation.notNull_notEmpty(null, null);
            case 417 -> stubValidation.validSelf(stubValidation);
            case 418 -> throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "Message for i-am-a-teapot-418");
            case 500 -> throw new NullPointerException("Message for null-pointer-exception-500");
            case 501 -> throw new UnsupportedOperationException("Message for unsupported-operation-exception-501");
            case 503 -> throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Message for service-unavailable-503");
            default -> throw new IllegalArgumentException("Message for illegal-argument-exception-" + pseudoStatus + " (try 400,401,403,404,416,417,418,500,501,503)");
        }
        return "";
    }

    /**
     * This nested class is solely used to generate validation exceptions for the stub test endpoint.
     */
    @SuppressWarnings({"FieldCanBeLocal", "SameParameterValue", "unused"})
    @Service
    @Validated
    static class StubValidation {
        @NotNull private final String member1 = null;
        @NotEmpty private final String member2 = null;

        void notNull_notEmpty(@NotNull final String param1, @NotEmpty final String param2) {
            // do nothing.
        }

        void validSelf(@Valid final StubValidation self) {
            // do nothing.
        }
    }
}
