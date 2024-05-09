package uk.gov.justice.laa.crime.dces.integration.service;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceServiceTest {

    @InjectMocks
    private TraceService traceService;

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @Test
    void whenTraceContextIsNull_thenTraceIdIsBlank() {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(null);
        assertEquals("traceId-not-found", traceService.getTraceId());
    }

    @Test
    void whenTraceContextIsNotNull_thenReturnValidTraceId() {
        String expectedTraceId = "65dc9547edbfcf0e5b19b8113a0b912c";
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(expectedTraceId);
        assertEquals(expectedTraceId, traceService.getTraceId());
    }

    @Test
    void whenCurrentTraceContextIsNull_thenTraceIdIsBlank() {
        when(tracer.currentSpan()).thenReturn(null);
        assertEquals("traceId-not-found", traceService.getTraceId());
    }
}