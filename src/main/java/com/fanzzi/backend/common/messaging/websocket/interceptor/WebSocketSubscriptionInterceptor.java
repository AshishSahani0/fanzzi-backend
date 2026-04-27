package com.fanzzi.backend.common.messaging.websocket.interceptor;

import com.fanzzi.backend.common.infrastructure.rate_limit.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    private final StringRedisTemplate redis;
    private final RateLimitService rateLimitService;

    private static final String CHANNEL_PREFIX = "/topic/channel/";
    private static final String MEMBERSHIP_PREFIX = "channel:membership:";
    private static final String RATE_PREFIX = "rate:ws:sub:";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // ✅ Only intercept SUBSCRIBE
        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs == null) {
            throw new IllegalArgumentException("No session");
        }

        String userId = (String) sessionAttrs.get("userId");

        if (userId == null) {
            throw new IllegalArgumentException("Unauthorized");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        // =====================================================
        // 🚫 RATE LIMIT (lightweight)
        // =====================================================
        rateLimitService.checkLimit(
                RATE_PREFIX + userId,
                30,
                Duration.ofSeconds(10)
        );

        // =====================================================
        // 🛡 CHANNEL ACCESS (REDIS ONLY)
        // =====================================================
        if (destination.startsWith(CHANNEL_PREFIX)) {

            String channelId = destination.substring(CHANNEL_PREFIX.length());

            String key = MEMBERSHIP_PREFIX + channelId + ":" + userId;

            Boolean isMember = redis.hasKey(key);

            if (!Boolean.TRUE.equals(isMember)) {
                throw new IllegalArgumentException("Access denied");
            }
        }

        return message;
    }
}