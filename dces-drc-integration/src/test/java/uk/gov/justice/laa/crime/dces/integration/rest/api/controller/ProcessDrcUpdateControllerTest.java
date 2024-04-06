package uk.gov.justice.laa.crime.dces.integration.rest.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.justice.laa.crime.dces.integration.model.drc.DrcDataRequest;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.tracing.TraceService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProcessDrcUpdateController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProcessDrcUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TraceService traceService;

    @MockBean
    private FdcService fdcService;

    @MockBean
    private ContributionService contributionService;

    private static final String CONTRIBUTION_URL = "/api/internal/v1/dces-drc-integration/process-drc-update/contribution";
    private static final String CONTRIBUTION_FDC_URL = "/api/internal/v1/dces-drc-integration/process-drc-update/fdc";


    @Test
    void testProcessContributionUpdateWhenDownstreamResponseIsValid() throws Exception {

        DrcDataRequest dataRequest = DrcDataRequest.builder()
                .concorId(99)
                .errorText("error 99")
                .build();
        String serviceResponse = "The request has been processed successfully";
        when(contributionService.processContributionUpdate(dataRequest)).thenReturn(serviceResponse);

        final ObjectMapper objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(dataRequest);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(serviceResponse));
    }

    @Test
    void testProcessContributionUpdateWhenDownstreamResponseIsNotValid() throws Exception {

        DrcDataRequest dataRequest = DrcDataRequest.builder()
                .concorId(9)
                .errorText("Failed to process")
                .build();
        String serviceResponse = "The request has failed to process";
        when(contributionService.processContributionUpdate(dataRequest)).thenReturn(serviceResponse);

        final ObjectMapper objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(dataRequest);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(serviceResponse));
    }

    @Test
    void testProcessFdcUpdateWhenDownstreamResponseIsValid() throws Exception {

        DrcDataRequest dataRequest = DrcDataRequest.builder()
                .concorId(99)
                .build();
        String serviceResponse = "The request has been processed successfully";
        when(fdcService.processFdcUpdate(dataRequest)).thenReturn(serviceResponse);

        final ObjectMapper objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(dataRequest);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(serviceResponse));
    }

    @Test
    void testProcessFdcUpdateWhenDownstreamResponseIsNotValid() throws Exception {

        DrcDataRequest dataRequest = DrcDataRequest.builder()
                .concorId(9)
                .errorText("Failed to process")
                .build();
        String serviceResponse = "The request has failed to process";
        when(fdcService.processFdcUpdate(dataRequest)).thenReturn(serviceResponse);

        final ObjectMapper objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(dataRequest);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(serviceResponse));
    }
}