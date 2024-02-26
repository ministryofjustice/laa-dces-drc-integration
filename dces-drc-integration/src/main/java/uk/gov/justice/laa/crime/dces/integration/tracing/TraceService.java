package uk.gov.justice.laa.crime.dces.integration.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TraceService {

    private final Tracer tracer;

    public String getTraceId() {
        final Span span = tracer.currentSpan();
        return Optional.ofNullable(span)
                .map(Span::context)
                .map(TraceContext::traceId)
                .orElse("traceId-not-found");
    }
}