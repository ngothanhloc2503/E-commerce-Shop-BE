package com.store.ecommerce.config.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@ConfigurationProperties(prefix = "rate-limit")
@Data
public class RateLimitProperties {
    private Map<String, Rule> rules = Map.of();  // key = keyPrefix từ annotation
    private Rule defaultRule = new Rule(10, 1, TimeUnit.MINUTES);

    @Data
    public static class Rule {
        private int maxRequests;
        private long timeWindow;
        private TimeUnit timeUnit;

        public Rule() {}
        public Rule(int maxRequests, long timeWindow, TimeUnit timeUnit) {
            this.maxRequests = maxRequests;
            this.timeWindow = timeWindow;
            this.timeUnit = timeUnit;
        }
    }
}
