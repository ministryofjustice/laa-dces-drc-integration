package uk.gov.justice.laa.crime.dces.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.justice.laa.crime.dces.integration.utils.RestTestUtils.getHttpHeaders;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class DcesDrcErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void verifyUsingErrorResponseWhenHittingBadRequestOnErrorController() throws Exception {
        // traceid is added to the response by the GlobalExceptionHandler.
        mockMvc.perform(get("/error").headers(getHttpHeaders()))
                .andExpect(status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(999));
    }
}