package com.store.ecommerce.config.ratelimit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    int maxRequests() default 10;

    long timeWindow() default 1;

    TimeUnit timeUnit() default TimeUnit.MINUTES;

    String keyPrefix() default "api";
}
