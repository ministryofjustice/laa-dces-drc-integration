package uk.gov.justice.laa.crime.dces.integration.controller;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
