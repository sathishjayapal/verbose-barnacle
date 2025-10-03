package me.sathish.sathishlogger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* me.sathish.logger..*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        Object[] args = pjp.getArgs();
        long startNs = System.nanoTime();
        if (log.isInfoEnabled()) {
            log.info("-> Enter {} args={} ", method, Arrays.toString(args));
        }
        try {
            Object result = pjp.proceed();
            long durMs = (System.nanoTime() - startNs) / 1_000_000;
            if (log.isInfoEnabled()) {
                log.info("<- Exit {} took={}ms result={}", method, durMs, summarize(result));
            }
            return result;
        } catch (Throwable ex) {
            long durMs = (System.nanoTime() - startNs) / 1_000_000;
            log.error("!! Exception in {} after {}ms: {}", method, durMs, ex.toString(), ex);
            throw ex;
        }
    }

    private String summarize(Object result) {
        if (result == null) return "null";
        String s = String.valueOf(result);
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
