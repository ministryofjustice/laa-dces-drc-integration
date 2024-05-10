package uk.gov.justice.laa.crime.dces.integration.configuration.wrapper;

import lombok.AllArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import uk.gov.justice.laa.crime.dces.integration.model.exception.TraceData;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;

import java.lang.reflect.Method;
import java.sql.Timestamp;

@ControllerAdvice
@AllArgsConstructor
public class ErrorResponseBodyWrapper implements ResponseBodyAdvice<Object> {

    private TraceService traceService;

    @Override
    public boolean supports(final MethodParameter returnType, final Class converterType) {
        final Method method = returnType.getMethod();
        return (method != null && method.isAnnotationPresent(WrapResponse.class)) || returnType.getContainingClass().isAnnotationPresent(WrapResponse.class);
    }

    @Override
    public Object beforeBodyWrite(final Object body, final MethodParameter returnType, final MediaType selectedContentType,
                                  final Class selectedConverterType, final ServerHttpRequest request, final ServerHttpResponse response) {

        if (!(body instanceof ResponseEntity)) {
            final ErrorResponse<Object> errorResponse = new ErrorResponse<>();
            errorResponse.setData(body);
            errorResponse.setTrace(TraceData.builder()
                    .traceId(traceService.getTraceId())
                    .timestamp(new Timestamp(System.currentTimeMillis()))
                    .build());
            return errorResponse;
        }
        return body;
    }
}