package uk.gov.justice.laa.crime.dces.integration.rest.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.justice.laa.crime.dces.integration.rest.common.ErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.rest.common.ErrorSubMessage;
import uk.gov.justice.laa.crime.dces.integration.rest.common.exception.DcesDrcValidationException;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final TraceService traceService;


    @ExceptionHandler(DcesDrcValidationException.class)
    public ResponseEntity<?> handleValidationException(final DcesDrcValidationException ex) {
        log.error("DcesDrcValidationException occurred.", ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(BAD_REQUEST, ex.getMessage())
                .subMessages(getSubMessages(ex))
                .build();
        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception ex) {
        log.error("Unexpected exception {} of instance {}", ex.getMessage(), ex.getClass(), ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(INTERNAL_SERVER_ERROR, ex.getMessage()).build();
        return new ResponseEntity<>(errorResponse, INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse.ErrorResponseBuilder initiateBasicErrorBuilder(final HttpStatus status,
                                                                         final String message) {
        return ErrorResponse.builder()
                .statusCode(status.value())
                .message(message)
                .traceId(traceService.getTraceId());
    }

    private static List<ErrorSubMessage> getSubMessages(final DcesDrcValidationException ex) {
        return ex.getValidationErrors()
                .entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .map(message -> ErrorSubMessage.builder()
                                .errorCode(VALIDATION_ERROR)
                                .field(entry.getKey())
                                .message(message).build())
                        .toList())
                .flatMap(List::stream)
                .toList();
    }
}