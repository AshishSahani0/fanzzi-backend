package com.fanzzi.backend.common.messaging.websocket.service;

import com.fanzzi.backend.common.messaging.websocket.model.PresencePayload;
import com.fanzzi.backend.common.messaging.websocket.model.WsEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WsPresenceService {

    private final StringRedisTemplate redis;
    private final WsSendService wsSendService;

    public WsPresenceService(StringRedisTemplate redis,
                             WsSendService wsSendService) {
        this.redis = redis;
        this.wsSendService = wsSendService;
    }

    private static final String KEY_PREFIX = "user:lastSeen:";
    private static final long ONLINE_THRESHOLD_MS = 30_000; // 30 sec

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }

    // =====================================================
    // 🟢 SET ONLINE (ON CONNECT / HEARTBEAT)
    // =====================================================
    public void setOnline(String userId) {

        long now = System.currentTimeMillis();

        redis.opsForValue().set(key(userId), String.valueOf(now));

        // 🔥 OPTIONAL: notify user (only if needed)
        wsSendService.sendToUser(userId, payload(userId, true));
    }

    // =====================================================
    // 🔁 HEARTBEAT UPDATE (LIGHTWEIGHT)
    // =====================================================
    public void refresh(String userId) {

        redis.opsForValue().set(
                key(userId),
                String.valueOf(System.currentTimeMillis())
        );
    }

    // =====================================================
    // 🔴 SET OFFLINE
    // =====================================================
    public void setOffline(String userId) {

        redis.delete(key(userId));

        wsSendService.sendToUser(userId, payload(userId, false));
    }

    // =====================================================
    // 🔍 CHECK ONLINE STATUS
    // =====================================================
    public boolean isOnline(String userId) {

        String value = redis.opsForValue().get(key(userId));

        if (value == null) return false;

        long lastSeen = Long.parseLong(value);

        return (System.currentTimeMillis() - lastSeen) < ONLINE_THRESHOLD_MS;
    }

    // =====================================================
    // 📦 PAYLOAD
    // =====================================================
    private Object payload(String userId, boolean online) {
        return new WsEvent<>(
                "PRESENCE",
                null,
                new PresencePayload(userId, online)
        );
    }
}