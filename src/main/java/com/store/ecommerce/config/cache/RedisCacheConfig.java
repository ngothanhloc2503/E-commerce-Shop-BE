package com.store.ecommerce.config.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${cache.ttl.home-products:300}")
    private long homeProductsTtl;

    @Value("${cache.ttl.product-by-alias:600}")
    private long productByAliasTtl;

    @Value("${cache.ttl.category-all:600}")
    private long categoryAllTtl;

    @Value("${cache.ttl.category-by-name:300}")
    private long categoryByNameTtl;

    @Value("${cache.ttl.default:300}")
    private long defaultTtl;

    @Lazy
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Bean
    public CommandLineRunner testRedisConnection(RedisTemplate<String, Object> redisTemplate,
                                                 RedisConnectionFactory factory) {
        return args -> {
            try {
                var connection = factory.getConnection();
                String pong = connection.ping();
                log.info("Redis PING response: {}", pong);

                try {
                    connection.serverCommands().flushAll();
                    log.info("Redis FLUSHALL succeeded - all old data cleared");
                } catch (Exception e) {
                    log.warn("FLUSHALL failed (Render may block it): {}", e.getMessage());
                    // Fallback: scan và xóa từng key bằng native connection
                    try {
                        var commands = connection.keyCommands();
                        Set<byte[]> keys = connection.keys("*".getBytes());
                        if (keys != null && !keys.isEmpty()) {
                            Long deleted = commands.del(keys.toArray(new byte[0][]));
                            log.info("Deleted {} old cache keys via DEL", deleted);
                        } else {
                            log.info("No existing cache keys found");
                        }
                    } catch (Exception e2) {
                        log.warn("Manual key deletion also failed: {}", e2.getMessage());
                        log.info("CacheErrorHandler will auto-evict corrupted keys on first access");
                    }
                }
                connection.close();
            } catch (Exception e) {
                log.error("Redis connection test FAILED!", e);
            }
        };
    }

    @Bean
    public GenericJackson2JsonRedisSerializer redisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.WRAPPER_ARRAY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        boolean useSsl = false;

        if (redisUrl != null && !redisUrl.isEmpty()) {
            try {
                java.net.URI uri = new java.net.URI(redisUrl);

                if ("rediss".equalsIgnoreCase(uri.getScheme())) {
                    useSsl = true;
                }

                if (uri.getHost() != null) {
                    config.setHostName(uri.getHost());
                }
                if (uri.getPort() != -1) {
                    config.setPort(uri.getPort());
                }

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isEmpty()) {
                    if (userInfo.contains(":")) {
                        String username = userInfo.substring(0, userInfo.indexOf(':'));
                        String password = userInfo.substring(userInfo.indexOf(':') + 1);
                        if (!username.isEmpty()) {
                            config.setUsername(username);
                        }
                        if (!password.isEmpty()) {
                            config.setPassword(password);
                        }
                    } else {
                        config.setPassword(userInfo);
                    }
                }

                log.info("Redis connecting to {}:{} (SSL={}) user={}",
                        config.getHostName(), config.getPort(), useSsl,
                        config.getUsername() != null ? config.getUsername() : "default");
            } catch (Exception e) {
                throw new IllegalStateException("Invalid REDIS_URL: " + redisUrl, e);
            }
        } else {
            config.setHostName(redisHost);
            config.setPort(redisPort);
            if (redisPassword != null && !redisPassword.isEmpty()) {
                config.setPassword(redisPassword);
            }
            useSsl = sslEnabled;
            log.info("Redis connecting to {}:{} (SSL={})", redisHost, redisPort, useSsl);
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(10));

        if (useSsl) {
            clientConfigBuilder.useSsl().disablePeerVerification();
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfigBuilder.build());
        factory.setValidateConnection(false);
        factory.setShareNativeConnection(true);

        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisSerializer) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(redisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisSerializer) {

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("home-products", defaultConfig
                .entryTtl(Duration.ofSeconds(homeProductsTtl)));
        cacheConfigurations.put("product-by-alias", defaultConfig
                .entryTtl(Duration.ofSeconds(productByAliasTtl)));
        cacheConfigurations.put("category-all", defaultConfig
                .entryTtl(Duration.ofSeconds(categoryAllTtl)));
        cacheConfigurations.put("category-by-name", defaultConfig
                .entryTtl(Duration.ofSeconds(categoryByNameTtl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET failed [cache={}, key={}]: {}", cache.getName(), key, exception.getMessage());
                logRootCause(exception);
                try {
                    String redisKey = cache.getName() + "::" + key;
                    Boolean deleted = redisTemplate.delete(redisKey);
                    log.info("Force-deleted corrupted key [redisKey={}, deleted={}]", redisKey, deleted);
                } catch (Exception e) {
                    log.warn("Failed to delete corrupted key: {}", e.getMessage());
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed [cache={}, key={}]: {}", cache.getName(), key, exception.getMessage());
                logRootCause(exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT failed [cache={}, key={}]: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR failed [cache={}]: {}", cache.getName(), exception.getMessage());
            }

            private void logRootCause(RuntimeException exception) {
                Throwable cause = exception.getCause();
                int depth = 0;
                while (cause != null && depth < 3) {
                    log.warn("  Caused by: [{}] {}", cause.getClass().getSimpleName(), cause.getMessage());
                    cause = cause.getCause();
                    depth++;
                }
            }
        };
    }
}