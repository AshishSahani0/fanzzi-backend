package com.fanzzi.backend.wallets.stars.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PurchaseGuardService {

    private final StringRedisTemplate redis;

    public boolean checkAndLock(String userId, String orderId) {

        String key = "purchase:lock:" + userId + ":" + orderId;

        Boolean success = redis.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(10));

        return success == null || !success;
    }
}
