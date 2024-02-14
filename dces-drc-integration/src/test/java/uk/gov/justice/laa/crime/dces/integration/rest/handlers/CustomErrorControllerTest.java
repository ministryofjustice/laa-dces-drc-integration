package uk.gov.justice.laa.crime.dces.integration.rest.handlers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.justice.laa.crime.dces.integration.rest.handlers.RestTestUtils.getHttpHeaders;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class CustomErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void verifyUsingErrorResponseWhenHittingBadRequestOnErrorController() throws Exception {
        mockMvc.perform(get("/error").headers(getHttpHeaders()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.statusCode")
                        .value(999))
                .andExpect(MockMvcResultMatchers.jsonPath("$.traceId")
                        .isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.correlationId")
                        .isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("None for path (). "));
    }
}