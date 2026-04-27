package com.fanzzi.backend.common.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    // =====================================================
    // 🔧 CONFIG FROM application.yml
    // =====================================================

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    // =====================================================
    // 🔌 CONNECTION FACTORY (POOL ENABLED)
    // =====================================================

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {

        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(host, port);

        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }

        // 🔥 CONNECTION POOL (VERY IMPORTANT)
        LettuceClientConfiguration clientConfig =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(3))
                        .shutdownTimeout(Duration.ZERO)
                        .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    // =====================================================
    // 🧠 SERIALIZER (SAFE + FAST)
    // =====================================================

    @Bean
    public GenericJackson2JsonRedisSerializer redisSerializer() {

        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );



        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    // =====================================================
    // ⚡ CACHE MANAGER
    // =====================================================

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory factory,
            GenericJackson2JsonRedisSerializer serializer
    ) {

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(30))
                        .computePrefixWith(name -> "fanzzi::" + name + "::")
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer())
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(serializer)
                        )
                        .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(
                        Map.of(
                                "my_channels", defaultConfig.entryTtl(Duration.ofMinutes(2)),
                                "joined_channels", defaultConfig.entryTtl(Duration.ofMinutes(2)),
                                "archived_channels", defaultConfig.entryTtl(Duration.ofMinutes(5)),

                                "channel_feed", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                                "user_sessions", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                                "channel_status_active", defaultConfig.entryTtl(Duration.ofMinutes(5))
                        )
                )
                .transactionAware()
                .build();
    }

    // =====================================================
    // 🔁 GENERIC TEMPLATE
    // =====================================================

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            GenericJackson2JsonRedisSerializer serializer
    ) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    // =====================================================
    // 🔤 STRING TEMPLATE (FASTEST)
    // =====================================================

    @Bean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory factory
    ) {
        return new StringRedisTemplate(factory);
    }
}