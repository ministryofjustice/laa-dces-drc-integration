package uk.gov.justice.laa.crime.dces.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@WebMvcTest(AckFromDrcController.class)
@AutoConfigureMockMvc(addFilters = false)
class AckFromDrcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private TraceService traceService;

    @MockitoBean
    private FdcService fdcService;

    @MockitoBean
    private ContributionService contributionService;

    private static final String CONTRIBUTION_URL = "/api/dces/v1/contribution";
    private static final String CONTRIBUTION_FDC_URL = "/api/dces/v1/fdc";


    @Test
    void testContributionWhenDownstreamResponseIsValid() throws Exception {

        Long serviceResponse = 1111L;
        ConcorContributionAckFromDrc concorContributionAckFromDrc = ConcorContributionAckFromDrc.of(99L, "error 99");

        when(contributionService.handleContributionProcessedAck(concorContributionAckFromDrc)).thenReturn(serviceResponse);

        final String requestBody = mapper.writeValueAsString(concorContributionAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testContributionWhenDownstreamResponseIsNotValid() throws Exception {


        ConcorContributionAckFromDrc concorContributionAckFromDrc = ConcorContributionAckFromDrc.of(9L, "Failed to process");
        var serviceResponse = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(contributionService.handleContributionProcessedAck(concorContributionAckFromDrc)).thenThrow(serviceResponse);

        final String requestBody = mapper.writeValueAsString(concorContributionAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testFdcWhenDownstreamResponseIsValid() throws Exception {

        FdcAckFromDrc fdcAckFromDrc = FdcAckFromDrc.of(99L, null);
        long serviceResponse = 1111L;
        when(fdcService.handleFdcProcessedAck(fdcAckFromDrc)).thenReturn(serviceResponse);

        final String requestBody = mapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testFdcWhenDownstreamResponseIsNotValid() throws Exception {

        FdcAckFromDrc fdcAckFromDrc = FdcAckFromDrc.of(9L, "Failed to process");
        var serviceResponse = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null,null,null);
        when(fdcService.handleFdcProcessedAck(fdcAckFromDrc)).thenThrow(serviceResponse);

        final String requestBody = mapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}