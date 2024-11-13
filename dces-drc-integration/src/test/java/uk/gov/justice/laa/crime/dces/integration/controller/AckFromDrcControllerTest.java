package uk.gov.justice.laa.crime.dces.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AckFromDrcController.class)
@AutoConfigureMockMvc(addFilters = false)
class AckFromDrcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TraceService traceService;

    @MockBean
    private FdcService fdcService;

    @MockBean
    private ContributionService contributionService;

    private static final String CONTRIBUTION_URL = "/api/dces/v1/contribution";
    private static final String CONTRIBUTION_FDC_URL = "/api/dces/v1/fdc";


    @Test
    void testContributionWhenDownstreamResponseIsValid() throws Exception {

        UpdateLogContributionRequest updateLogContributionRequest = UpdateLogContributionRequest.builder()
                .concorId(99)
                .errorText("error 99")
                .build();
        Integer serviceResponse = 1111;
        when(contributionService.processContributionUpdate(updateLogContributionRequest)).thenReturn(serviceResponse);

        ConcorContributionAckFromDrc concorContributionAckFromDrc = ConcorContributionAckFromDrc.of(99, "error 99");
        final String requestBody = mapper.writeValueAsString(concorContributionAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testContributionWhenDownstreamResponseIsNotValid() throws Exception {

        UpdateLogContributionRequest updateLogContributionRequest = UpdateLogContributionRequest.builder()
                .concorId(9)
                .errorText("Failed to process")
                .build();
        var serviceResponse = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(contributionService.processContributionUpdate(updateLogContributionRequest)).thenThrow(serviceResponse);

        ConcorContributionAckFromDrc concorContributionAckFromDrc = ConcorContributionAckFromDrc.of(9, "Failed to process");
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
                .fdcId(99)
                .build();
        Integer serviceResponse = 1111;
        when(fdcService.handleFdcProcessedAck(fdcProcessedRequest)).thenReturn(serviceResponse);

        FdcAckFromDrc fdcAckFromDrc = FdcAckFromDrc.of(99, null);

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
                .fdcId(9)
                .errorText("Failed to process")
                .build();
        var serviceResponse = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null,null,null);
        when(fdcService.handleFdcProcessedAck(fdcProcessedRequest)).thenThrow(serviceResponse);

        FdcAckFromDrc fdcAckFromDrc = FdcAckFromDrc.of(9, "Failed to process");
        final String requestBody = mapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}