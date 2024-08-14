package uk.gov.justice.laa.crime.dces.integration.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

    private final MeterRegistry registry;

    public CountedAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Pointcut("@within(Counted)")
    public void classCounter() {
    }

    @After("classCounter()")
    public void classMethodCounter(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String parentClassName = joinPoint.getSignature().getDeclaringType().getSuperclass().getName();

        Counter counter = Counter.builder(methodName)
                .tags(methodName, className, parentClassName)
                .description("Number of calls to " + methodName)
                .register(registry);
        counter.increment();

    }

}