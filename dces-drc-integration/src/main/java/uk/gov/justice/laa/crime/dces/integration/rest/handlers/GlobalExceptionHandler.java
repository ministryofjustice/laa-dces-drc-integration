package uk.gov.justice.laa.crime.dces.integration.rest.handlers;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.justice.laa.crime.dces.integration.rest.common.ErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.rest.common.ErrorSubMessage;
import uk.gov.justice.laa.crime.dces.integration.rest.common.exception.DcesDrcValidationException;
import uk.gov.justice.laa.crime.dces.integration.tracing.TraceService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    public static final String VALIDATION_FAILURE = "Validation failure.";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private final TraceService traceService;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(final IllegalArgumentException ex) {
        log.error("IllegalArgumentException occurred.", ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(BAD_REQUEST, ex.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperationException(final UnsupportedOperationException ex) {
        log.error("UnsupportedOperationException was thrown", ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(BAD_REQUEST, ex.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<ErrorResponse> handleUnrecognizedPropertyException(final UnrecognizedPropertyException ex) {
        log.error("UnrecognizedPropertyException was thrown", ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(BAD_REQUEST, VALIDATION_FAILURE)
                .subMessages(List.of(ErrorSubMessage.builder()
                        .errorCode("INVALID_JSON_UNRECOGNIZED_PROPERTY")
                        .field(ex.getPropertyName())
                        .message("Detected unrecognized field '" + ex.getPropertyName() + "' in JSON for class "
                                + ex.getReferringClass().getSimpleName() + ".")
                        .build()))
                .build();
        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(final HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException was thrown", ex);
        if (ex.getCause() != null && ex.getCause() instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
            return handleUnrecognizedPropertyException((UnrecognizedPropertyException) ex.getCause());
        }
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(BAD_REQUEST, VALIDATION_FAILURE)
                .subMessages(List.of(ErrorSubMessage.builder()
                        .errorCode("INVALID_JSON")
                        .message(ex.getMostSpecificCause().getMessage())
                        .build()))
                .build();
        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(final MethodArgumentNotValidException ex) {
        log.error("MethodArgumentNotValidException occurred.", ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(BAD_REQUEST, VALIDATION_FAILURE)
                .subMessages(ex.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> ErrorSubMessage.builder()
                                .errorCode(VALIDATION_ERROR)
                                .field(fieldError.getField())
                                .value(Objects.toString(fieldError.getRejectedValue(), null))
                                .message(fieldError.getDefaultMessage())
                                .build())
                        .toList())
                .build();
        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(final ConstraintViolationException ex) {
        log.error("ConstraintViolationException occurred.", ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(BAD_REQUEST, VALIDATION_FAILURE)
                .subMessages(ex.getConstraintViolations().stream().map(
                        violation -> ErrorSubMessage.builder()
                                .errorCode(VALIDATION_ERROR)
                                .field(getFieldFromViolation(violation))
                                .value(Objects.toString(violation.getInvalidValue(), null))
                                .message(violation.getMessage())
                                .build()).toList())
                .build();
        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(DcesDrcValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(final DcesDrcValidationException ex) {
        log.error("SavahValidationErrorException occurred.", ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(BAD_REQUEST, ex.getMessage())
                .subMessages(getSubMessages(ex))
                .build();
        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception ex) {
        log.error("Unexpected exception {} of instance {}", ex.getMessage(), ex.getClass(), ex);
        final ErrorResponse errorResponse = initiateBasicErrorBuilder(INTERNAL_SERVER_ERROR, ex.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse.ErrorResponseBuilder initiateBasicErrorBuilder(final HttpStatus status,
                                                                         final String message) {
        return ErrorResponse.builder()
                .statusCode(status.value())
                .message(message)
                .traceId(traceService.getTraceId())
                .correlationId(UUID.randomUUID().toString());
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

    private static String getFieldFromViolation(final ConstraintViolation<?> violation) {
        final String fullPath = violation.getPropertyPath().toString();
        if (StringUtils.isBlank(fullPath)) {
            return StringUtils.EMPTY;
        }
        final String[] split = fullPath.split("\\.");
        return split[split.length - 1];
    }
}