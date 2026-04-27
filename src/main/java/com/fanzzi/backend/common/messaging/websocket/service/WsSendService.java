package com.fanzzi.backend.common.messaging.websocket.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WsSendService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHANNEL_PREFIX = "channel.";
    private static final String USER_PREFIX = "user.";
    private static final String CHANNEL_SUBSCRIBERS_PREFIX = "channel.subscribers.";

    public WsSendService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void sendChannelEvent(String channelId, Object payload) {
        redisTemplate.convertAndSend(CHANNEL_PREFIX + channelId, payload);
    }

    public void sendToUser(String userId, Object payload) {
        redisTemplate.convertAndSend(USER_PREFIX + userId, payload);
    }

    public void sendToChannelSubscribers(String channelId, Object payload) {

        // 🔥 Option 1 (FASTEST): topic-based (recommended)
        redisTemplate.convertAndSend(CHANNEL_SUBSCRIBERS_PREFIX + channelId, payload);

        // 🔥 Option 2 (ADVANCED - per user push)
        // If you maintain subscriber list in Redis:
        /*
        Set<Object> subscribers = redisTemplate.opsForSet()
                .members("channel:subs:" + channelId);

        if (subscribers != null) {
            for (Object userId : subscribers) {
                sendToUser(userId.toString(), payload);
            }
        }
        */
    }


}