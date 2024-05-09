package uk.gov.justice.laa.crime.dces.integration.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import uk.gov.justice.laa.crime.dces.integration.rest.common.ErrorResponse;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;

import java.util.Map;


@Slf4j
@RestController
@AllArgsConstructor
public class DcesDrcErrorController implements ErrorController {

    private final TraceService traceService;
    public static final String MESSAGE_KEY = "message";
    public static final String STATUS_KEY = "status";
    public static final String ERROR_KEY = "error";
    public static final String PATH_KEY = "path";

    private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @RequestMapping("/error")
    public ErrorResponse handleError(final WebRequest webRequest, final Exception exception) {
        log.error("Error occurred while processing web request ", exception);
        final Map<String, Object> errorAttributesMap = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());
        return createErrorResponse(errorAttributesMap);
    }

    private ErrorResponse createErrorResponse(final Map<String, Object> errorAttributes) {
        final int statusCode = (Integer) errorAttributes.get(STATUS_KEY);
        final String errorMessage = errorAttributes.getOrDefault(MESSAGE_KEY, StringUtils.EMPTY).toString();
        final String errorCode = errorAttributes.getOrDefault(ERROR_KEY, StringUtils.EMPTY).toString();
        final String path = errorAttributes.getOrDefault(PATH_KEY, StringUtils.EMPTY).toString();

        return ErrorResponse.builder()
                .statusCode(statusCode)
                .traceId(traceService.getTraceId())
                .message(errorCode + " for path (" + path + "). " + errorMessage)
                .build();
    }
}