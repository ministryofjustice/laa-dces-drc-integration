package uk.gov.justice.laa.crime.dces.integration.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.buildContribAck;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.buildFdcAck;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc.ConcorContributionAckData;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc.FdcAckData;
import uk.gov.justice.laa.crime.dces.integration.model.ProcessingReport;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionAckService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcAckService;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;


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
    private FdcAckService fdcAckService;

    @MockitoBean
    private ContributionAckService contributionAckService;

    private static final String CONTRIBUTION_URL = "/api/dces/v1/contribution";
    private static final String CONTRIBUTION_FDC_URL = "/api/dces/v1/fdc";


    @Test
    void testContributionWhenDownstreamResponseIsValid() throws Exception {

        Long serviceResponse = 1111L;
        ConcorContributionAckFromDrc concorContributionAckFromDrc = buildContribAck(99L);

        when(contributionAckService.handleContributionProcessedAck(concorContributionAckFromDrc)).thenReturn(serviceResponse);

        final String requestBody = mapper.writeValueAsString(concorContributionAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testContributionWhenDownstreamResponseIsNotValid() throws Exception {

        ConcorContributionAckFromDrc concorContributionAckFromDrc = buildContribAck(99L);

        var serviceResponse = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(contributionAckService.handleContributionProcessedAck(concorContributionAckFromDrc)).thenThrow(serviceResponse);

        final String requestBody = mapper.writeValueAsString(concorContributionAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testFdcWhenDownstreamResponseIsValid() throws Exception {

        FdcAckFromDrc fdcAckFromDrc = buildFdcAck(99L);

        long serviceResponse = 1111L;
        when(fdcAckService.handleFdcProcessedAck(fdcAckFromDrc)).thenReturn(serviceResponse);

        final String requestBody = mapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testFdcWhenDownstreamResponseIsNotValid() throws Exception {

        FdcAckFromDrc fdcAckFromDrc = buildFdcAck(99L);
        var serviceResponse = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null,null,null);
        when(fdcAckService.handleFdcProcessedAck(fdcAckFromDrc)).thenThrow(serviceResponse);

        final String requestBody = mapper.writeValueAsString(fdcAckFromDrc);

        mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_FDC_URL))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Nested
    @DisplayName("Validates incoming requests")
    class ValidationTests {

        @Nested
        @DisplayName("Validates FDC contribution requests")
        class ValidateFDCTests {

            @Test
            @DisplayName("FDC requests must have a data field present.")
            void missingDataProcessingFdcAckRequestReturnsBadRequestStatus() throws Exception {
                FdcAckFromDrc invalidAck = new FdcAckFromDrc(null, Map.of());
                String request = mapper.writeValueAsString(invalidAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            @ParameterizedTest
            @DisplayName("Data must have all mandatory fields present.")
            @MethodSource("provideFdcAckDataWithNulls")
            void fdcAckDataWithMissingFieldsProcessingRequestReturnsBadRequestStatus(
                FdcAckData fdcAckData)
                throws Exception {
                FdcAckFromDrc ackWithNullFields = new FdcAckFromDrc(fdcAckData, Map.of());
                String request = mapper.writeValueAsString(ackWithNullFields);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            static Stream<Arguments> provideFdcAckDataWithNulls() {
                return Stream.of(
                    Arguments.of(new FdcAckData(null, 1L, new ProcessingReport("title", "detail"))),
                    Arguments.of(
                        new FdcAckData(123L, null, new ProcessingReport("title", "detail"))),
                    Arguments.of(new FdcAckData(123L, 1L, null))
                );
            }

            @ParameterizedTest
            @DisplayName("FDC and MAAT IDs must be positive integers.")
            @MethodSource("provideFdcAckDataWithNegativeIds")
            void fdcAckDataWithNegativeIdsProcessingRequestReturnsBadRequestStatus(
                FdcAckData fdcAckData)
                throws Exception {
                FdcAckFromDrc ackWithNegativeId = new FdcAckFromDrc(fdcAckData, Map.of());
                String request = mapper.writeValueAsString(ackWithNegativeId);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            static Stream<Arguments> provideFdcAckDataWithNegativeIds() {
                return Stream.of(
                    Arguments.of(new FdcAckData(-1L, 1L, new ProcessingReport("title", "detail"))),
                    Arguments.of(
                        new FdcAckData(123L, -1L, new ProcessingReport("title", "detail"))),
                    Arguments.of(new FdcAckData(0L, 1L, new ProcessingReport("title", "detail"))),
                    Arguments.of(new FdcAckData(123L, 0L, new ProcessingReport("title", "detail")))
                );
            }

            @ParameterizedTest
            @DisplayName("processingReport must have all mandatory fields present.")
            @MethodSource("provideProcessingReportWithMissingFields")
            void processingReportWithMissingFieldsProcessingRequestReturnsBadRequestStatus(
                ProcessingReport processingReport)
                throws Exception {
                FdcAckFromDrc ackWithInvalidProcessingReport = new FdcAckFromDrc(
                    new FdcAckData(123L, 1234L, processingReport), Map.of());
                String request = mapper.writeValueAsString(ackWithInvalidProcessingReport);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(request).contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            public static Stream<Arguments> provideProcessingReportWithMissingFields() {
                return Stream.of(
                    Arguments.of(new ProcessingReport(null, "detail")),
                    Arguments.of(new ProcessingReport("title", null))
                );
            }

            @ParameterizedTest
            @DisplayName("Titles must be between 1 and 200 characters.")
            @MethodSource("provideProcessingReportsWithInvalidTitle")
            void processingReportWithInvalidTitleProcessingRequestReturnsBadRequestStatus(
                ProcessingReport processingReport)
                throws Exception {

                FdcAckFromDrc fdcAck = new FdcAckFromDrc(
                    new FdcAckData(123L, 1234L, processingReport), Map.of());
                String fdcRequest = mapper.writeValueAsString(fdcAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(fdcRequest).contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            public static Stream<Arguments> provideProcessingReportsWithInvalidTitle() {
                return Stream.of(
                    Arguments.of(new ProcessingReport(
                        "ThisTitleIsFarTooLongOnlyUpToTwoHundredCharactersAreAllowed".repeat(4),
                        "detail")),
                    Arguments.of(new ProcessingReport("", "detail"))
                );
            }

            @ParameterizedTest
            @DisplayName("Details must be ISO‑8601 date‑times that are explicitly in UTC, using either Z or +00:00.")
            @MethodSource("provideProcessingReportsWithDetailsAndExpectedResults")
            void processingReportValidatingDetailsOnlyAcceptsValidTimestamps(
                ProcessingReport processingReport, ResultMatcher expectedResult)
                throws Exception {
                FdcAckFromDrc fdcAck = new FdcAckFromDrc(
                    new FdcAckData(123L, 1234L, processingReport), Map.of());
                String fdcRequest = mapper.writeValueAsString(fdcAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(fdcRequest).contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(expectedResult);
            }

            public static Stream<Arguments> provideProcessingReportsWithDetailsAndExpectedResults() {
                return Stream.of(
                    Arguments.of(new ProcessingReport("title", "2025-11-06T14:54:45+00:00"),
                        status().isOk()),
                    Arguments.of(new ProcessingReport("title", "2025-11-06T14:54:45Z"),
                        status().isOk()),
                    Arguments.of(new ProcessingReport("title", "invalid detail"),
                        status().isBadRequest()),
                    Arguments.of(new ProcessingReport("title", "2024-01-10T12:45:30+03:00"),
                        status().isBadRequest()),
                    Arguments.of(new ProcessingReport("title", "2024-01-10T12:45:30"),
                        status().isBadRequest())
                );
            }

            @Test
            @DisplayName("Validation errors are returned to the caller in the response.")
            void missingFieldOnRequestReturnMessageDetailingError() throws Exception {
                FdcAckFromDrc invalidAck = new FdcAckFromDrc(null, Map.of());
                String request = mapper.writeValueAsString(invalidAck);

                mockMvc.perform(
                        MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                            .content(request)
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.data").value("data cannot be null."));

            }

            @Test
            @DisplayName("Multiple validation errors are all returned to the caller.")
            void missingFieldsOnRequestReturnMessageContainsAllErrors() throws Exception {
                FdcAckData ackData = new FdcAckData(-1L, 111L,
                    new ProcessingReport("title", "invalid detail"));
                FdcAckFromDrc invalidAck = new FdcAckFromDrc(ackData, Map.of());
                String request = mapper.writeValueAsString(invalidAck);

                mockMvc.perform(
                        MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                            .content(request)
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.data.report.detail").value(
                        "detail must be ISO 8601 format explicitly in UTC, using either Z or +00:00."))
                    .andExpect(jsonPath("$.errors.data.fdcId").value(
                        "fdcId must be positive."));
            }
        }

        @Nested
        @DisplayName("Validates Concor contribution requests")
        class ValidatesContributionTests {

            @Test
            @DisplayName("Contribution requests must have a data field present.")
            void missingDataProcessingContributionRequestReturnsBadRequestStatus()
                throws Exception {
                ConcorContributionAckFromDrc invalidAck = new ConcorContributionAckFromDrc(null,
                    Map.of());
                String request = mapper.writeValueAsString(invalidAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            @ParameterizedTest
            @DisplayName("Data must have all mandatory fields present.")
            @MethodSource("provideContributionAckDataWithNulls")
            void contributionAckDataWithMissingFieldsProcessingRequestReturnsBadRequestStatus(
                ConcorContributionAckData contributionAckData)
                throws Exception {
                ConcorContributionAckFromDrc ackWithNullFields = new ConcorContributionAckFromDrc(
                    contributionAckData, Map.of());
                String request = mapper.writeValueAsString(ackWithNullFields);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            static Stream<Arguments> provideContributionAckDataWithNulls() {
                return Stream.of(
                    Arguments.of(new ConcorContributionAckData(null, 1L,
                        new ProcessingReport("title", "detail"))),
                    Arguments.of(new ConcorContributionAckData(123L, null,
                        new ProcessingReport("title", "detail"))),
                    Arguments.of(new ConcorContributionAckData(123L, 1L, null))
                );
            }

            @ParameterizedTest
            @DisplayName("Contribution and MAAT IDs must be positive integers.")
            @MethodSource("provideContributionAckDataWithNegativeIds")
            void contributionAckDataWithNegativeIdsProcessingRequestReturnsBadRequestStatus(
                ConcorContributionAckData contributionAckData)
                throws Exception {
                ConcorContributionAckFromDrc ackWithNegativeId = new ConcorContributionAckFromDrc(
                    contributionAckData, Map.of());
                String request = mapper.writeValueAsString(ackWithNegativeId);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            static Stream<Arguments> provideContributionAckDataWithNegativeIds() {
                return Stream.of(
                    Arguments.of(new ConcorContributionAckData(-1L, 1L,
                        new ProcessingReport("title", "detail"))),
                    Arguments.of(new ConcorContributionAckData(123L, -1L,
                        new ProcessingReport("title", "detail"))),
                    Arguments.of(new ConcorContributionAckData(0L, 1L,
                        new ProcessingReport("title", "detail"))),
                    Arguments.of(new ConcorContributionAckData(123L, 0L,
                        new ProcessingReport("title", "detail")))
                );
            }

            @ParameterizedTest
            @DisplayName("processingReport must have all mandatory fields present.")
            @MethodSource("provideProcessingReportWithMissingFields")
            void processingReportWithMissingFieldsProcessingContributionReturnsBadRequestStatus(
                ProcessingReport processingReport)
                throws Exception {
                ConcorContributionAckFromDrc ackWithInvalidProcessingReport = new ConcorContributionAckFromDrc(
                    new ConcorContributionAckData(123L, 1234L, processingReport), Map.of());
                String request = mapper.writeValueAsString(ackWithInvalidProcessingReport);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                        .content(request).contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            public static Stream<Arguments> provideProcessingReportWithMissingFields() {
                return Stream.of(
                    Arguments.of(new ProcessingReport(null, "detail")),
                    Arguments.of(new ProcessingReport("title", null))
                );
            }

            @ParameterizedTest
            @DisplayName("Titles must be between 1 and 200 characters.")
            @MethodSource("provideProcessingReportsWithInvalidTitle")
            void processingReportWithInvalidTitleProcessingRequestReturnsBadRequestStatus(
                ProcessingReport processingReport)
                throws Exception {
                ConcorContributionAckFromDrc concorAck = new ConcorContributionAckFromDrc(
                    new ConcorContributionAckData(123L, 1234L, processingReport), Map.of());
                String concorRequest = mapper.writeValueAsString(concorAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                        .content(concorRequest).contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest());
            }

            public static Stream<Arguments> provideProcessingReportsWithInvalidTitle() {
                return Stream.of(
                    Arguments.of(new ProcessingReport(
                        "ThisTitleIsFarTooLongOnlyUpToTwoHundredCharactersAreAllowed".repeat(4),
                        "detail")),
                    Arguments.of(new ProcessingReport("", "detail"))
                );
            }

            @ParameterizedTest
            @DisplayName("Details must be ISO‑8601 date‑times that are explicitly in UTC, using either Z or +00:00.")
            @MethodSource("provideProcessingReportsWithDetailsAndExpectedResults")
            void processingReportValidatingDetailsOnlyAcceptsValidTimestamps(
                ProcessingReport processingReport, ResultMatcher expectedResult)
                throws Exception {
                ConcorContributionAckFromDrc concorAck = new ConcorContributionAckFromDrc(
                    new ConcorContributionAckData(123L, 1234L, processingReport), Map.of());
                String concorRequest = mapper.writeValueAsString(concorAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                        .content(concorRequest).contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(expectedResult);
            }

            public static Stream<Arguments> provideProcessingReportsWithDetailsAndExpectedResults() {
                return Stream.of(
                    Arguments.of(new ProcessingReport("title", "2025-11-06T14:54:45+00:00"),
                        status().isOk()),
                    Arguments.of(new ProcessingReport("title", "2025-11-06T14:54:45Z"),
                        status().isOk()),
                    Arguments.of(new ProcessingReport("title", "invalid detail"),
                        status().isBadRequest()),
                    Arguments.of(new ProcessingReport("title", "2024-01-10T12:45:30+03:00"),
                        status().isBadRequest()),
                    Arguments.of(new ProcessingReport("title", "2024-01-10T12:45:30"),
                        status().isBadRequest())
                );
            }

            @Test
            @DisplayName("Validation errors are returned to the caller in the response.")
            void missingFieldOnRequestReturnMessageDetailingError() throws Exception {
                ConcorContributionAckFromDrc invalidAck = new ConcorContributionAckFromDrc(null,
                    Map.of());
                String request = mapper.writeValueAsString(invalidAck);

                MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                            .content(request)
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.data").value("data cannot be null."))
                    .andReturn();
            }

            @Test
            @DisplayName("Multiple validation errors are all returned to the caller.")
            void missingFieldsOnRequestReturnsMessageContainsAllErrors() throws Exception {
                ConcorContributionAckFromDrc invalidAck = new ConcorContributionAckFromDrc(
                    new ConcorContributionAckData(-1L, 111L,
                        new ProcessingReport("title", "invalid detail")),
                    Map.of());
                String request = mapper.writeValueAsString(invalidAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.data.report.detail").value(
                        "detail must be ISO 8601 format explicitly in UTC, using either Z or +00:00."))
                    .andExpect(jsonPath("$.errors.data.concorContributionId").value(
                        "concorContributionId must be positive."));
            }
        }
    }
}