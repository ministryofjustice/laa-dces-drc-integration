package uk.gov.justice.laa.crime.dces.integration.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class CountedAspect {

    public static final String METRIC_PREFIX = "api_calls_count";

    @Pointcut("@within(Counted)")
    public void classCounter() {
    }

    @After("classCounter()")
    public void classMethodCounter(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();

        Counter counter = Counter.builder(METRIC_PREFIX)
                .tags("method", methodName, "class", className)
                .register(Metrics.globalRegistry);
        counter.increment();
    }
}