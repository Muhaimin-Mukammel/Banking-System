package com.banking.annotation.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

@Component
public class RateLimitService {
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(100000)
            .build();

    public Bucket resolveBucket(String key, int capacity, int refillTokens, Long refillPeriodSeconds){
        return buckets.get(key, k -> createBucket(capacity, refillTokens, refillPeriodSeconds));
    }

    public Bucket createBucket(int capacity, int refillTokens, Long refillPeriodSeconds){
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.greedy(refillTokens, Duration.ofSeconds(refillPeriodSeconds)));
        return Bucket.builder().addLimit(limit).build();
    }
}
