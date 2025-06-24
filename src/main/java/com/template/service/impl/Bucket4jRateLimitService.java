package com.template.service.impl;

import com.template.service.RateLimitService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
@Slf4j
public class Bucket4jRateLimitService implements RateLimitService {

    @Value("${app.rate-limit.otp.max-requests:5}")
    private int maxRequests;

    @Value("${app.rate-limit.otp.window-minutes:15}")
    private int windowMinutes;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key) {
        try {
            Bucket bucket = getBucket(key);
            return bucket.tryConsume(1);
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // In case of error, allow the request (fail-open)
            return true;
        }
    }

    @Override
    public long getRemainingRequests(String key) {
        try {
            Bucket bucket = getBucket(key);
            return bucket.getAvailableTokens();
        } catch (Exception e) {
            log.error("Error getting remaining requests for key: {}", key, e);
            return 0;
        }
    }

    @Override
    public long getTimeUntilReset(String key) {
        try {
            Bucket bucket = getBucket(key);
            return bucket.getAvailableTokens() < maxRequests ? Duration.ofMinutes(windowMinutes).toSeconds() : 0;
        } catch (Exception e) {
            log.error("Error getting time until reset for key: {}", key, e);
            return 0;
        }
    }

    private Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(maxRequests,
                        Refill.intervally(maxRequests, Duration.ofMinutes(windowMinutes))))
                .build());
    }

    // Alternative configuration with sliding window
    private Bucket getSlidingWindowBucket(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.simple(maxRequests, Duration.ofMinutes(windowMinutes)))
                .build());
    }
}