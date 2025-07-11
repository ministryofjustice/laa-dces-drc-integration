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
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcProcessedRequest;
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

        ContributionProcessedRequest contributionProcessedRequest = ContributionProcessedRequest.builder()
                .concorId(99L)
                .errorText("error 99")
                .build();
        Long serviceResponse = 1111L;
        when(contributionService.handleContributionProcessedAck(contributionProcessedRequest)).thenReturn(serviceResponse);

        ConcorContributionAckFromDrc concorContributionAckFromDrc = ConcorContributionAckFromDrc.of(99L, "error 99");
        final String requestBody = mapper.writeValueAsString(concorContributionAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testContributionWhenDownstreamResponseIsNotValid() throws Exception {

        ContributionProcessedRequest contributionProcessedRequest = ContributionProcessedRequest.builder()
                .concorId(9L)
                .errorText("Failed to process")
                .build();
        var serviceResponse = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(contributionService.handleContributionProcessedAck(contributionProcessedRequest)).thenThrow(serviceResponse);

        ConcorContributionAckFromDrc concorContributionAckFromDrc = ConcorContributionAckFromDrc.of(9L, "Failed to process");
        final String requestBody = mapper.writeValueAsString(concorContributionAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testFdcWhenDownstreamResponseIsValid() throws Exception {

        FdcProcessedRequest fdcProcessedRequest = FdcProcessedRequest.builder()
                .fdcId(99L)
                .build();
        long serviceResponse = 1111L;
        when(fdcService.handleFdcProcessedAck(fdcProcessedRequest)).thenReturn(serviceResponse);

        FdcAckFromDrc fdcAckFromDrc = FdcAckFromDrc.of(99L, null);

        final String requestBody = mapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testFdcWhenDownstreamResponseIsNotValid() throws Exception {

        FdcProcessedRequest fdcProcessedRequest = FdcProcessedRequest.builder()
                .fdcId(9L)
                .errorText("Failed to process")
                .build();
        var serviceResponse = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null,null,null);
        when(fdcService.handleFdcProcessedAck(fdcProcessedRequest)).thenThrow(serviceResponse);

        FdcAckFromDrc fdcAckFromDrc = FdcAckFromDrc.of(9L, "Failed to process");
        final String requestBody = mapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}