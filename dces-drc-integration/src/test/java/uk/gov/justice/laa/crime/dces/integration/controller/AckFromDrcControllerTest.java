package uk.gov.justice.laa.crime.dces.integration.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.justice.laa.crime.dces.integration.controller.error.ValidationProblemDetail.VALIDATION_ERROR_TYPE;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.buildContribAck;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.buildFdcAck;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setup() {
        when(traceService.getTraceId()).thenReturn(UUID.randomUUID().toString());
    }

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

        private static final String VALID_TITLE = "Success";
        private static final String VALID_DETAIL = Instant.now().toString();

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
                    .andExpectAll(validationErrorResponseExpectations());
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
                    .andExpectAll(validationErrorResponseExpectations());
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
                    .andExpectAll(validationErrorResponseExpectations());
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
                ProcessingReport processingReport, String fieldInError, String errorMessage)
                throws Exception {
                FdcAckFromDrc ackWithInvalidProcessingReport = new FdcAckFromDrc(
                    new FdcAckData(123L, 1234L, processingReport), Map.of());
                String request = mapper.writeValueAsString(ackWithInvalidProcessingReport);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(request).contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andExpectAll(validationErrorResponseExpectations())
                        .andExpectAll(validationFieldErrorExpectations(fieldInError, errorMessage));
            }

            public static Stream<Arguments> provideProcessingReportWithMissingFields() {
                return Stream.of(
                    Arguments.of(new ProcessingReport(null, VALID_DETAIL), "data.report.title", "Title must not be blank."),
                    Arguments.of(new ProcessingReport(VALID_TITLE, null), "data.report.detail", "Detail must not be null.")
                );
            }

            @ParameterizedTest
            @DisplayName("Titles must be not blank and not greater than 200 characters.")
            @MethodSource("provideProcessingReportsWithInvalidTitle")
            void processingReportWithInvalidTitleProcessingRequestReturnsBadRequestStatus(
                ProcessingReport processingReport, String fieldInError, String errorMessage)
                throws Exception {

                FdcAckFromDrc fdcAck = new FdcAckFromDrc(
                    new FdcAckData(123L, 1234L, processingReport), Map.of());
                String fdcRequest = mapper.writeValueAsString(fdcAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(fdcRequest).contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andExpectAll(validationErrorResponseExpectations())
                        .andExpectAll(validationFieldErrorExpectations(fieldInError, errorMessage));
            }

            public static Stream<Arguments> provideProcessingReportsWithInvalidTitle() {
                return Stream.of(
                    Arguments.of(new ProcessingReport(
                        "ThisTitleIsFarTooLongOnlyUpToTwoHundredCharactersAreAllowed".repeat(4),
                        VALID_DETAIL), "data.report.title", "Title cannot be more than 200 characters."),
                    Arguments.of(new ProcessingReport("", VALID_DETAIL), "data.report.title", "Title must not be blank."),
                    Arguments.of(new ProcessingReport(" ", VALID_DETAIL), "data.report.title", "Title must not be blank."),
                    Arguments.of(new ProcessingReport("\n", VALID_DETAIL), "data.report.title", "Title must not be blank.")
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
                    .andExpectAll(validationErrorResponseExpectations())
                    .andExpect(jsonPath("$.errors[0].message").value("data cannot be null."));

            }

            @Test
            @DisplayName("Multiple validation errors are all returned to the caller.")
            void missingFieldsOnRequestReturnMessageContainsAllErrors() throws Exception {
                FdcAckData ackData = new FdcAckData(-1L, 111L,
                    new ProcessingReport("title", "invalid detail"));
                FdcAckFromDrc invalidAck = new FdcAckFromDrc(ackData, Map.of());
                String request = mapper.writeValueAsString(invalidAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_FDC_URL)
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpectAll(validationErrorResponseExpectations())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("data.fdcId", "data.report.detail")))
                    .andExpect(jsonPath("$.errors[*].message",
                        containsInAnyOrder(
                            "FDC ID must be positive.",
                            "Detail must be ISO 8601 format explicitly in UTC, using either Z or +00:00."
                        )));
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
                    .andExpectAll(validationErrorResponseExpectations());
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
                    .andExpectAll(validationErrorResponseExpectations());
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
                    .andExpectAll(validationErrorResponseExpectations());
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
                ProcessingReport processingReport, String fieldInError, String errorMessage)
                throws Exception {
                ConcorContributionAckFromDrc ackWithInvalidProcessingReport = new ConcorContributionAckFromDrc(
                    new ConcorContributionAckData(123L, 1234L, processingReport), Map.of());
                String request = mapper.writeValueAsString(ackWithInvalidProcessingReport);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                        .content(request).contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpectAll(validationErrorResponseExpectations())
                    .andExpectAll(validationFieldErrorExpectations(fieldInError, errorMessage));
            }

            public static Stream<Arguments> provideProcessingReportWithMissingFields() {
                return Stream.of(
                    Arguments.of(new ProcessingReport(null, VALID_DETAIL), "data.report.title", "Title must not be blank."),
                    Arguments.of(new ProcessingReport(VALID_TITLE, null), "data.report.detail", "Detail must not be null.")
                );
            }

            @ParameterizedTest
            @DisplayName("Titles must not be blank and not greater than 200 characters.")
            @MethodSource("provideProcessingReportsWithInvalidTitle")
            void processingReportWithInvalidTitleProcessingRequestReturnsBadRequestStatus(
                ProcessingReport processingReport, String fieldInError, String errorMessage)
                throws Exception {
                ConcorContributionAckFromDrc concorAck = new ConcorContributionAckFromDrc(
                    new ConcorContributionAckData(123L, 1234L, processingReport), Map.of());
                String concorRequest = mapper.writeValueAsString(concorAck);

                mockMvc.perform(MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                    .content(concorRequest).contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpectAll(validationErrorResponseExpectations())
                    .andExpectAll(validationFieldErrorExpectations(fieldInError, errorMessage));
            }

            public static Stream<Arguments> provideProcessingReportsWithInvalidTitle() {
                return Stream.of(
                    Arguments.of(new ProcessingReport(
                        "ThisTitleIsFarTooLongOnlyUpToTwoHundredCharactersAreAllowed".repeat(4),
                        VALID_DETAIL), "data.report.title", "Title cannot be more than 200 characters."),
                    Arguments.of(new ProcessingReport("", VALID_DETAIL), "data.report.title", "Title must not be blank."),
                    Arguments.of(new ProcessingReport(" ", VALID_DETAIL), "data.report.title", "Title must not be blank."),
                    Arguments.of(new ProcessingReport("\n", VALID_DETAIL), "data.report.title", "Title must not be blank."),
                    Arguments.of(new ProcessingReport(" \n\t ", VALID_DETAIL), "data.report.title", "Title must not be blank.")
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

                mockMvc.perform(
                        MockMvcRequestBuilders.post(CONTRIBUTION_URL)
                            .content(request)
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpectAll(validationErrorResponseExpectations())
                    .andExpect(jsonPath("$.errors[0].field").value("data"))
                    .andExpect(jsonPath("$.errors[0].message").value("data cannot be null."));
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
                    .andExpectAll(validationErrorResponseExpectations())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("data.concorContributionId", "data.report.detail")))
                    .andExpect(jsonPath("$.errors[*].message",
                        containsInAnyOrder(
                            "Concor Contribution ID must be positive.",
                            "Detail must be ISO 8601 format explicitly in UTC, using either Z or +00:00."
                        )));

            }
        }
    }

    public static ResultMatcher[] validationErrorResponseExpectations() {
        return new ResultMatcher[]{
            status().isBadRequest(),
            jsonPath("$.type").value(VALIDATION_ERROR_TYPE.toString()),
            jsonPath("$.title").value("Bad Request"),
            jsonPath("$.traceId").exists()
        };
    }

    public static ResultMatcher[] validationFieldErrorExpectations(String fieldName, String message) {
        return new ResultMatcher[]{
            jsonPath("$.errors[*].field").value(fieldName),
            jsonPath("$.errors[*].message").value(message)
        };
    }
}