package uk.gov.justice.laa.crime.dces.integration.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CountedAspect {

    private static final Logger logger = LoggerFactory.getLogger(CountedAspect.class);
    private final MeterRegistry registry;

    public CountedAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Pointcut("@within(Counted)")
    public void countedClasses() {
    }

    @After("countedClasses()")
    public void afterCountedMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        Counter counter = Counter.builder(methodName)
                .description("Number of calls to " + methodName)
                .register(registry);
        counter.increment();

        logger.info("Method {} was called, current count is {}", methodName, counter.count());

    }

}