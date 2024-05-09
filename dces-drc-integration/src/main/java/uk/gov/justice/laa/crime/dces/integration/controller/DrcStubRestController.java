package uk.gov.justice.laa.crime.dces.integration.controller;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.crime.dces.integration.model.external.SendContributionFileDataToExternalRequest;

import java.util.Random;

/**
 * This is a Stub to simulate the request and response from a 3rd Party (DRC). Class will be removed once we have
 * a client to process a DRC.
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/drc/external")
@SuppressWarnings("squid:S2245") // Safe to use random here, as it's not being used in any fashion beyond a return value.
public class DrcStubRestController {

    private static final Random random = new Random();

    public static int getRandomZeroOrOne() {
        return random.nextInt(2);
    }

    @PostMapping(value = "/fdc")
    public Boolean fdc(@NotEmpty @RequestBody final SendContributionFileDataToExternalRequest dataRequest) {
        log.info("Request received from DRC to update FDC {}", dataRequest);
        return getRandomZeroOrOne() == 0;
    }

    @PostMapping(value = "/contribution")
    public Boolean contribution(@NotEmpty @RequestBody final SendContributionFileDataToExternalRequest dataRequest) {
        log.info("DrcStubRestController Stub returing a response for input {}", dataRequest);
        return getRandomZeroOrOne() == 0;
    }
}
