package com.store.ecommerce.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Dùng Simple in-memory cache cho test, không cần Redis
        return new ConcurrentMapCacheManager(
                "category-all", "categories", "category-by-name",
                "products", "home-products", "product-by-alias", "products-page"
        );
    }
}