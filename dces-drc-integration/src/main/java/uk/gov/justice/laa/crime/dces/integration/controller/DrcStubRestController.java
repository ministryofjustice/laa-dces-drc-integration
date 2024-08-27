package uk.gov.justice.laa.crime.dces.integration.controller;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.crime.dces.integration.model.SendContributionFileDataToDrcRequest;

import java.util.Random;

/**
 * This is a Stub to simulate the request and response from a 3rd Party (DRC). Class will be removed once we have
 * a client to process a DRC.
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/external/v1/drc")
@SuppressWarnings("squid:S2245") // Safe to use random here, as it's not being used in any fashion beyond a return value.
public class DrcStubRestController {

    private static final Random random = new Random();

    public static int getRandomZeroOrOne() {
        return random.nextInt(2);
    }

    @PostMapping(value = "/fdc")
    public Boolean fdc(@NotNull @RequestBody final SendContributionFileDataToDrcRequest dataRequest) {
        log.info("Request received from DRC to update FDC {}", dataRequest);
        return getRandomZeroOrOne() == 0;
    }

    @PostMapping(value = "/contribution")
    public Boolean contribution(@NotNull @RequestBody final SendContributionFileDataToDrcRequest dataRequest) {
        log.info("DrcStubRestController Stub returing a response for input {}", dataRequest);
        return getRandomZeroOrOne() == 0;
    }

    @GetMapping(value = "/test")
    public String testSecure() {
        log.info("Get Request received to secure test");
        return "mTLS (secure) Test Get Success";
    }

    @PostMapping(value = "/test")
    public String test(@NotNull @RequestBody final SendContributionFileDataToDrcRequest dataRequest) {
        log.info("Request received from DRC to test FDC {}", dataRequest);
        return "Request received from DRC to test FDC " + dataRequest;
    }


}
