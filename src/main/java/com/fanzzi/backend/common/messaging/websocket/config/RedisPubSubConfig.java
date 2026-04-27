package com.fanzzi.backend.common.messaging.websocket.config;

import com.fanzzi.backend.common.messaging.websocket.listener.RedisWsSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.*;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final RedisConnectionFactory connectionFactory;
    private final RedisWsSubscriber subscriber;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 🔥 CHANNEL EVENTS
        container.addMessageListener(subscriber, new PatternTopic("channel.*"));

        // 🔥 USER EVENTS
        container.addMessageListener(subscriber, new PatternTopic("user.*"));

        return container;
    }
}