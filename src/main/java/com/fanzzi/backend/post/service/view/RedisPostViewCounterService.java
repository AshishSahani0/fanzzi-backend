package com.fanzzi.backend.post.service.view;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPostViewCounterService {

    private final StringRedisTemplate redis;

    private static final String VIEW_COUNT_KEY = "post:view:count:";
    private static final String VIEW_USERS_KEY = "post:view:users:";
    private static final String ACTIVE_POSTS = "post:view:active";

    private static final Duration TTL = Duration.ofHours(24);

    @Async
    public void incrementView(String postId, String userId) {

        if (postId == null || postId.isBlank()) return;

        try {
            String finalUserId = (userId != null) ? userId : "anon:" + Thread.currentThread().getId();

            String userKey = VIEW_USERS_KEY + postId;

            Long added = redis.opsForSet().add(userKey, finalUserId);

            if (added != null && added > 0) {
                redis.opsForValue().increment(VIEW_COUNT_KEY + postId);
            }

            redis.opsForSet().add(ACTIVE_POSTS, postId);

            redis.expire(userKey, TTL);
            redis.expire(VIEW_COUNT_KEY + postId, TTL);

        } catch (Exception e) {
            log.warn("View increment failed postId={} userId={}", postId, userId, e);
        }
    }

    // optional lightweight dedupe
    public boolean tryUniqueView(String key) {

        Boolean success = redis.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(5));

        return Boolean.TRUE.equals(success);
    }
}