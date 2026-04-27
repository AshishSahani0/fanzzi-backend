package com.fanzzi.backend.common.messaging.websocket.config;

import com.fanzzi.backend.common.messaging.websocket.interceptor.WebSocketAuthInterceptor;
import com.fanzzi.backend.common.messaging.websocket.interceptor.WebSocketSubscriptionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final WebSocketSubscriptionInterceptor subscriptionInterceptor;

    // =====================================================
    // 🚀 INBOUND CHANNEL (CLIENT → SERVER)
    // =====================================================
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration
                .interceptors(subscriptionInterceptor)
                .taskExecutor()
                .corePoolSize(8)
                .maxPoolSize(32)
                .queueCapacity(1000);
    }

    // =====================================================
    // 🚀 OUTBOUND CHANNEL (SERVER → CLIENT)
    // =====================================================
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration
                .taskExecutor()
                .corePoolSize(8)
                .maxPoolSize(32)
                .queueCapacity(1000);
    }

    // =====================================================
    // 🔌 ENDPOINT
    // =====================================================
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authInterceptor);

        // ❌ REMOVE SockJS (IMPORTANT FOR SCALE)
    }

    // =====================================================
    // 📡 MESSAGE BROKER
    // =====================================================
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler());

        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    // =====================================================
    // ❤️ HEARTBEAT THREAD POOL
    // =====================================================
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(16); // 🔥 CRITICAL (not 2)
        scheduler.setThreadNamePrefix("ws-heartbeat-");

        scheduler.initialize();
        return scheduler;
    }
}