package com.banking.annotation.ratelimit;

import com.banking.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        int capacity = rateLimit != null ? rateLimit.capacity() : 60;
        int refillTokens = rateLimit != null ? rateLimit.refillTokens() : 60;
        Long refillPeriodSecond = rateLimit != null ? rateLimit.refillPeriodSeconds() : 60;

        String identity = resolveIdentity(request);
        String bucketKey = identity + "|" + handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();

        Bucket bucket = rateLimitService.resolveBucket(bucketKey, capacity, refillTokens, refillPeriodSecond);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if(probe.isConsumed()){
            response.addHeader("Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        Long waitSeconds = probe.getNanosToWaitForRefill() / 1000000000;
        throw new RateLimitExceededException("Rate limit exceeded. Try again later.", waitSeconds);
    }

    private String resolveIdentity(HttpServletRequest request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())){
            return "user: " + auth.getName();
        }

        return "ip: " + request.getRemoteAddr();
    }
}
