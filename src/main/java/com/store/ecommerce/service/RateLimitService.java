package com.store.ecommerce.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    @Autowired(required = false)
    private RedissonClient redissonClient;

    private final Cache<String, Boolean> initializedLimiters = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100000)
            .build();

    public RateLimitService() {}

    public RateLimitService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void checkRateLimit(String clientIp, String methodKey,
                               int maxRequests, long timeWindow, TimeUnit timeUnit) {

        if (redissonClient == null) {
            return;
        }

        String rateLimiterKey = "rate_limit:" + clientIp + ":" + methodKey;

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);

        // [FIX 2C] Kiểm tra local cache trước khi gọi xuống Redis
        if (initializedLimiters.getIfPresent(rateLimiterKey) == null) {
            rateLimiter.trySetRate(
                    RateType.OVERALL,
                    maxRequests,
                    timeWindow,
                    convertToRateIntervalUnit(timeUnit)
            );
            initializedLimiters.put(rateLimiterKey, Boolean.TRUE);
        }

        if (!rateLimiter.tryAcquire()) {
            throw new com.store.ecommerce.config.ratelimit.RateLimitExceededException(
                    String.format("Rate limit exceeded. Max %d requests per %d %s",
                            maxRequests, timeWindow, timeUnit)
            );
        }
    }

    private RateIntervalUnit convertToRateIntervalUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS, MICROSECONDS, MILLISECONDS -> RateIntervalUnit.MILLISECONDS;
            case SECONDS -> RateIntervalUnit.SECONDS;
            case MINUTES -> RateIntervalUnit.MINUTES;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.DAYS;
        };
    }

    public void clearCache() {
        initializedLimiters.invalidateAll();
        // Xóa toàn bộ key rate limit cũ trên Redis
        // redissonClient.getKeys().deleteByPattern("rate_limit:*");
    }
}