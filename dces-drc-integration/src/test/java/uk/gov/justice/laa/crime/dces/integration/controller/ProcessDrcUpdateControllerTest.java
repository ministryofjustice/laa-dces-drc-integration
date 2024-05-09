package uk.gov.justice.laa.crime.dces.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.drc.UpdateLogFdcRequest;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;

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

        UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
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

        UpdateLogContributionRequest dataRequest = UpdateLogContributionRequest.builder()
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

        UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
                .fdcId(99)
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

        UpdateLogFdcRequest dataRequest = UpdateLogFdcRequest.builder()
                .fdcId(9)
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