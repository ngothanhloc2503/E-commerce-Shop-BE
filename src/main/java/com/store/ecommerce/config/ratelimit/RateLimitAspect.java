package com.store.ecommerce.config.ratelimit;

import com.store.ecommerce.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Around("@annotation(com.store.ecommerce.config.ratelimit.RateLimit)")
    public Object rateLimitInterceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String clientIp = getClientIp(request);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimitAnnotation = method.getAnnotation(RateLimit.class);

        if (rateLimitAnnotation != null) {
            String keyPrefix = rateLimitAnnotation.keyPrefix();

            RateLimitProperties.Rule rule = rateLimitProperties.getRules().getOrDefault(
                    keyPrefix,
                    rateLimitProperties.getDefaultRule()
            );

            String methodKey = method.getDeclaringClass().getName() + "#" + method.getName();

            rateLimitService.checkRateLimit(
                    clientIp,
                    methodKey,
                    rule.getMaxRequests(),
                    rule.getTimeWindow(),
                    rule.getTimeUnit()
            );
        }

        return joinPoint.proceed();
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (rateLimitProperties.getTrustedProxies().contains(remoteAddr)) {
            String cfIp = request.getHeader("CF-Connecting-IP");
            if (cfIp != null && !cfIp.isEmpty()) {
                return cfIp;
            }

            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) {
                return realIp;
            }

            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isEmpty()) {
                return xfHeader.split(",")[0].trim();
            }
        }

        return remoteAddr;
    }
}