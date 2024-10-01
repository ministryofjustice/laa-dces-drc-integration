package uk.gov.justice.laa.crime.dces.integration.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This is a simple temporary controller to handle some test endpoints.
 * TODO: Remove or disable this controller once we're happy with everything.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(TempTestController.PREFIX)
public class TempTestController {
    static final String PREFIX = "/api/dces/test";
    private final StubValidation stubValidation;
    private final ObjectMapper objectMapper;
    private final DrcClient drcClient;

    /**
     * Check we have connectivity with almost no side effects (just a log line).
     */
    @GetMapping(value = "/")
    public String getTest() {
        log.info("Received GET {}", PREFIX);
        return "GET Test Successful";
    }

    /**
     * Forward to the '/hello' service at Advantis, that returns 'hello there!' as plain text.
     */
    @GetMapping(value = "/hello")
    public String forwardHello() {
        log.info("Received GET {}/hello", PREFIX);
        return drcClient.hello();
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
        fdc.setId(BigInteger.valueOf(94));
        fdc.setMaatId(BigInteger.valueOf(105));
        fdc.setSentenceDate(DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2023-05-21"));
        fdc.setCalculationDate(null);
        fdc.setAgfsTotal(null);
        fdc.setLgfsTotal(BigDecimal.valueOf(2000.0));
        fdc.setFinalCost(BigDecimal.valueOf(3500.0));
        return fdc;
    }

    /**
     * Forward a provided FDC record to the fdc endpoint at Advantis.
     */
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

    /**
     * Throw one of a number of different exception types so the caller can check how it is reported to API consumers.
     * You csn also use a pseudoStatus out of range (e.g. 504) to check validation of controller method arguments.
     */
    @GetMapping(value = "/error/{pseudoStatus}")
    public String getTestError(@PathVariable("pseudoStatus") @Min(400) @Max(503) int pseudoStatus) {
        log.info("Received GET {}/error/{}", PREFIX, pseudoStatus);
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
