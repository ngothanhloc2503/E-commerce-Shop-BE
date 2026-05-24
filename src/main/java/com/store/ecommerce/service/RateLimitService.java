package com.store.ecommerce.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.store.ecommerce.config.ratelimit.RateLimit;
import com.store.ecommerce.config.ratelimit.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {
    // Tự động xóa key sau 1 giờ không được truy cập
    private final Cache<String, Queue<Long>> requestLogs = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();


    public void checkRateLimit(String clientIp, String methodKey,
                               int maxRequests, long timeWindow, TimeUnit timeUnit) {
        // Tạo key duy nhất: ip + method + keyPrefix (cho phép gộp nếu muốn)
        String fullKey = clientIp + ":" + methodKey;
        long now = System.currentTimeMillis();
        long windowMillis = timeUnit.toMillis(timeWindow);
        long cutoff = now - windowMillis;

        // Lấy hoặc tạo queue mới, dùng compute để atomic
        Queue<Long> queue = requestLogs.get(fullKey, k -> new ConcurrentLinkedQueue<>());

        // Synchronize trên queue để đảm bảo check-and-add atomic
        synchronized (queue) {
            // Dùng vòng lặp thay vì removeIf để dừng sớm (nhưng vẫn O(n))
            // Thực tế nếu queue chỉ chứa tối đa maxRequests phần tử thì O(maxRequests) là chấp nhận được
            queue.removeIf(t -> t < cutoff);

            if (queue.size() >= maxRequests) {
                throw new RateLimitExceededException(
                        String.format("Rate limit exceeded. Max %d requests per %d %s",
                                maxRequests, timeWindow, timeUnit)
                );
            }
            queue.add(now);
        }
    }

    // Dọn cache thủ công nếu cần test
    public void clearCache() {
        requestLogs.invalidateAll();
    }
}