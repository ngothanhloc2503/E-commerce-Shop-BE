package com.store.ecommerce.config.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
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
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;
import java.time.Duration;

/**
 * Redis Cache Configuration
 */
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
                log.warn("Redis PING failed: {}. Caching sẽ không hoạt động.", e.getMessage());
            }
            return factory;
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo Redis connection từ URL", e);
        }
    }

    // ========================================================================
    // 2) Jackson2JsonRedisSerializer<Object> + ObjectMapper(EVERYTHING + WRAPPER_ARRAY)
    // ========================================================================
    @Bean
    public Jackson2JsonRedisSerializer<Object> redisSerializer() {
        ObjectMapper mapper = new ObjectMapper();

        // Detect fields trực tiếp
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        // Cho phép tất cả types
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        // EVERYTHING + WRAPPER_ARRAY = luôn wrap ["type", value]
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.WRAPPER_ARRAY);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // ✅ Constructor mới: truyền ObjectMapper + Class trực tiếp
        return new Jackson2JsonRedisSerializer<>(mapper, Object.class);
    }

    // ========================================================================
    // 3) RedisTemplate
    // ========================================================================
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            Jackson2JsonRedisSerializer<Object> redisSerializer) {

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
            Jackson2JsonRedisSerializer<Object> redisSerializer) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(redisSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    // ========================================================================
    // 5) CacheErrorHandler - SELF-HEALING
    // ========================================================================
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET error [{}::{}]: {}", cache.getName(), key, exception.getMessage());
                try {
                    RedisTemplate<String, Object> template = redisTemplateProvider.getIfAvailable();
                    if (template != null) {
                        String redisKey = cache.getName() + "::" + key;
                        Boolean deleted = template.delete(redisKey);
                        log.info("Self-healing: deleted corrupted key '{}' → deleted={}", redisKey, deleted);
                    }
                } catch (Exception e) {
                    log.warn("Self-healing delete failed: {}", e.getMessage());
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
    // 6) CommandLineRunner - Xóa cache cũ + Round-trip test
    // ========================================================================
    @Bean
    public CommandLineRunner clearOldCacheAndTest(
            RedisConnectionFactory connectionFactory,
            Jackson2JsonRedisSerializer<Object> redisSerializer) {
        return args -> {
            // Xóa cache cũ
            try {
                var connection = connectionFactory.getConnection();
                var keys = connection.commands().keys("*".getBytes());
                if (keys != null && !keys.isEmpty()) {
                    Long deleted = connection.commands().del(keys.toArray(new byte[0][]));
                    log.info("★ Startup: deleted {} old cache keys ★", deleted);
                } else {
                    log.info("Startup: no old cache keys found");
                }
                connection.close();
            } catch (Exception e) {
                log.warn("Startup: could not clear old cache: {}", e.getMessage());
            }
        };
    }
}
