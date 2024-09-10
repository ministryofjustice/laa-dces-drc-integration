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
import uk.gov.justice.laa.crime.dces.integration.maatapi.exception.MaatApiClientException;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogContributionRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;
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

        ContributionAckFromDrc contributionAckFromDrc = ContributionAckFromDrc.of(99, "error 99");
        final ObjectMapper objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(contributionAckFromDrc);

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
        var serviceResponse = new MaatApiClientException(HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        when(contributionService.processContributionUpdate(updateLogContributionRequest)).thenThrow(serviceResponse);

        ContributionAckFromDrc contributionAckFromDrc = ContributionAckFromDrc.of(9, "Failed to process");
        final ObjectMapper objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(contributionAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500));
    }

    @Test
    void testFdcWhenDownstreamResponseIsValid() throws Exception {

        UpdateLogFdcRequest updateLogFdcRequest = UpdateLogFdcRequest.builder()
                .fdcId(99)
                .build();
        Integer serviceResponse = 1111;
        when(fdcService.processFdcUpdate(updateLogFdcRequest)).thenReturn(serviceResponse);

        FdcAckFromDrc fdcAckFromDrc = FdcAckFromDrc.of(99, null);

        final ObjectMapper objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testFdcWhenDownstreamResponseIsNotValid() throws Exception {

        UpdateLogFdcRequest updateLogFdcRequest = UpdateLogFdcRequest.builder()
                .fdcId(9)
                .errorText("Failed to process")
                .build();
        var serviceResponse = new MaatApiClientException(HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        when(fdcService.processFdcUpdate(updateLogFdcRequest)).thenThrow(serviceResponse);

        FdcAckFromDrc fdcAckFromDrc = FdcAckFromDrc.of(9, "Failed to process");
        final ObjectMapper objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500));
    }
}