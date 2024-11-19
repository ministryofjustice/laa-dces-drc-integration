package uk.gov.justice.laa.crime.dces.integration.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcValidationException;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final TraceService traceService;

    @ExceptionHandler(DcesDrcValidationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ProblemDetail handleValidationException(final DcesDrcValidationException ex) {
        log.info("DcesDrcValidationException occurred.", ex);
        return addTraceId(validationProblemDetail(ex, ex.getValidationErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ProblemDetail handleValidationException(final MethodArgumentNotValidException ex) {
        log.info("MethodArgumentNotValidException occurred.", ex);
        return addTraceId(validationProblemDetail(ex, ex.getFieldErrors().stream()
                .collect(Collectors.groupingBy(FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())))));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ProblemDetail handleValidationException(final HandlerMethodValidationException ex) {
        log.info("HandlerMethodValidationException occurred.", ex);
        return addTraceId(validationProblemDetail(ex, ex.getAllValidationResults().stream()
                .collect(Collectors.groupingBy(pvr -> pvr.getMethodParameter().getParameterName(),
                        Collectors.flatMapping(pvr -> pvr.getResolvableErrors().stream()
                                .map(MessageSourceResolvable::getDefaultMessage), Collectors.toList())))));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ProblemDetail handleValidationException(final ConstraintViolationException ex) {
        log.info("ConstraintViolationException occurred.", ex);
        return addTraceId(validationProblemDetail(ex, ex.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(cv -> cv.getPropertyPath().toString(),
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(final Exception ex) {
        log.warn("Unexpected {} occurred.", ex.getClass().getName(), ex);
        ProblemDetail pd = null;
        if (ex instanceof ErrorResponse resp) { // includes ResponseStatusException.
            pd = resp.getBody();
        } else if (ex instanceof RestClientResponseException resp) { // includes HttpServerErrorException.
            try {
                pd = resp.getResponseBodyAs(ProblemDetail.class);
            } catch (Exception e) {
                // getResponseBodyAs can throw IllegalStateException, RestClientException, etc. if not
                // created by Spring internal error handlers or there is a problem with deserialization.
                // If it does, then the ProblemDetail will be created in the `if (pd == null)` below.
            }
            if (pd == null) {
                pd = ProblemDetail.forStatusAndDetail(resp.getStatusCode(), resp.getMessage());
            }
        } else if (ex instanceof WebClientResponseException resp) {
            try {
                pd = resp.getResponseBodyAs(ProblemDetail.class);
            } catch (Exception e) {
                // getResponseBodyAs can throw IllegalStateException, etc. if not created by
                // ClientResponse.createError()/createException() or there is a problem with deserialization.
                // If it does, then the ProblemDetail will be created in the `if (pd == null)` below.
            }
            if (pd == null) {
                pd = ProblemDetail.forStatusAndDetail(resp.getStatusCode(), resp.getMessage());
            }
        } else {
            pd = ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, ex.getMessage());
        }
        //assert pd != null;
        return ResponseEntity.status(pd.getStatus()).body(addTraceId(pd));
    }

    private ProblemDetail validationProblemDetail(final Exception ex, final Map<String, List<String>> validationErrors) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, ex.getMessage());
        problemDetail.setProperty("validationErrors", validationErrors);
        return problemDetail;
    }

    private ProblemDetail addTraceId(ProblemDetail problemDetail) {
        problemDetail.setProperty("traceId", traceService.getTraceId());
        return problemDetail;
    }
}
