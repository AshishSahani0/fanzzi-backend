package com.fanzzi.backend.post.service.poll;

import com.fanzzi.backend.post.dto.Poll;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PollCacheService {

    private final RedisTemplate<String, Object> redis;

    private static final String KEY = "poll:cache:";
    private static final Duration TTL = Duration.ofMinutes(10);

    public Poll get(String postId) {
        Object obj = redis.opsForValue().get(key(postId));
        return obj instanceof Poll ? (Poll) obj : null;
    }

    public void put(String postId, Poll poll) {
        if (poll == null) return;
        redis.opsForValue().set(key(postId), poll, TTL);
    }

    public void evict(String postId) {
        redis.delete(key(postId));
    }

    private String key(String postId) {
        return KEY + postId;
    }
}