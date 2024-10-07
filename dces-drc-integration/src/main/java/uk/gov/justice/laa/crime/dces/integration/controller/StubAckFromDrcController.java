package uk.gov.justice.laa.crime.dces.integration.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;

import java.util.Random;

/**
 * This is a stub to simulate the endpoint that the DRC would call to asynchronously acknowledge their receipt of an
 * FDC or Concor Contribution record (see class AckFromDrcController for the real thing).
 */
@ConditionalOnProperty(prefix = "feature", name = "stub-ack-endpoints")
@RequestMapping("/api/dces/v1-stub")
@RequiredArgsConstructor
@RestController
@Slf4j
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
    public void concorContribution(@NotNull @RequestBody final ConcorContributionAckFromDrc concorContributionAckFromDrc) {
        log.info("Received concorContribution acknowledgement from DRC {}", concorContributionAckFromDrc);
        if (getRandomZeroOrOne() != 0) {
            throw new MaatApiClientException(HttpStatus.BAD_REQUEST, "Bad Request");
        }
    }
}
