package com.store.ecommerce.config.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisCacheConfig implements CachingConfigurer {

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${spring.data.redis.timeout:10000ms}")
    private Duration timeout;

    @Value("#{${spring.cache.ttl:{default:300}}}")
    private Map<String, Integer> ttlMap;

    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    public RedisCacheConfig(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    // ========================================================================
    // 1) LettuceConnectionFactory
    // ========================================================================
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalStateException("app.redis.enabled=true nhưng spring.data.redis.url trống!");
        }
        try {
            URI uri = new URI(redisUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null && userInfo.contains(":")) {
                int colonIndex = userInfo.indexOf(':');
                username = userInfo.substring(0, colonIndex);
                password = userInfo.substring(colonIndex + 1);
            } else if (userInfo != null) {
                password = userInfo;
            }

            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(host);
            config.setPort(port);
            if (password != null) config.setPassword(password);
            if (username != null) config.setUsername(username);

            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                    LettuceClientConfiguration.builder().commandTimeout(timeout);
            if (sslEnabled || redisUrl.startsWith("rediss://")) {
                clientBuilder.useSsl();
                log.info("Redis SSL enabled");
            }

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientBuilder.build());
            factory.afterPropertiesSet();

            try {
                factory.getConnection().ping();
                log.info("Redis connection SUCCESS - PING → PONG");
            } catch (Exception e) {
                log.warn("Redis PING failed: {}.", e.getMessage());
            }
            return factory;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ========================================================================
    // 2) Jackson2JsonRedisSerializer
    // ========================================================================
    @Bean
    public RedisSerializer<Object> redisSerializer() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.store.ecommerce.")
                .allowIfBaseType("java.util.")
                .allowIfBaseType("java.lang.")
                .allowIfBaseType("java.time.")
                .allowIfBaseType("[Ljava.lang.")
                .allowIfBaseType("[Lcom.store.ecommerce.")
                .allowIfBaseType("org.springframework.data.domain.")
                .build();

        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());

        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    // ========================================================================
    // 3) RedisTemplate
    // ========================================================================
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<Object> redisSerializer) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisSerializer);
        template.setHashValueSerializer(redisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    // ========================================================================
    // 4) CacheManager
    // ========================================================================
    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<Object> redisSerializer) {

        int defaultTtl = ttlMap.getOrDefault("default", 300);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtl))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(redisSerializer))
                .disableCachingNullValues();

        // Cấu hình riêng cho từng cache name
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        for (Map.Entry<String, Integer> entry : ttlMap.entrySet()) {
            if (!"default".equals(entry.getKey())) {
                cacheConfigurations.put(
                        entry.getKey(),
                        defaultConfig.entryTtl(Duration.ofSeconds(entry.getValue()))
                );
            }
        }

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    // ========================================================================
    // 5) CacheErrorHandler
    // ========================================================================
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET error [{}::{}]: {}", cache.getName(), key, exception.getMessage());
                try {
                    cache.evict(key);
                    log.info("Self-healing: evicted corrupted key '{}' from cache '{}'", key, cache.getName());
                } catch (Exception e) {
                    log.warn("Self-healing evict failed: {}", e.getMessage());
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT error [{}::{}]: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT error [{}::{}]: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR error [{}]: {}", cache.getName(), exception.getMessage());
            }
        };
    }

    // ========================================================================
    // 6) CommandLineRunner
    // ========================================================================
    @Bean
    public CommandLineRunner clearOldCacheAndTest(RedisTemplate<String, Object> redisTemplate) {
        return args -> {
            log.info("★ Startup: Kiểm tra kết nối Redis... ★");
            try {
                String pong = redisTemplate.getConnectionFactory().getConnection().ping();
                log.info("Redis PING success: {}", pong);
                log.info("★ Startup: TTL sẽ tự động làm mới dữ liệu. ★");
            } catch (Exception e) {
                log.warn("Startup: Redis connection/check failed: {}", e.getMessage());
            }
        };
    }
}