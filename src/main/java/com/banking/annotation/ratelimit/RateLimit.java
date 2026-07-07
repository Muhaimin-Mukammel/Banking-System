package com.banking.annotation.ratelimit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int capacity() default 60;
    int refillTokens() default 60;
    long refillPeriodSeconds() default 60;
}
