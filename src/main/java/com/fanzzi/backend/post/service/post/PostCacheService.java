package com.fanzzi.backend.post.service.post;

import com.fanzzi.backend.post.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final RedisTemplate<String, Object> redis;

    private static final String KEY = "post:%s";
    private static final Duration TTL = Duration.ofHours(6);

    private String key(String postId) {
        return KEY.formatted(postId);
    }

    // =========================
    // GET
    // =========================
    public PostResponse get(String postId) {

        Object obj = redis.opsForValue().get(key(postId));

        if (obj instanceof PostResponse p) {
            return p;
        }

        return null;
    }

    // =========================
    // PUT
    // =========================
    public void put(PostResponse post) {

        if (post == null || post.getId() == null) return;

        redis.opsForValue().set(
                key(post.getId()),
                post,
                TTL
        );
    }

    // =========================
    // DELETE
    // =========================
    public void delete(String postId) {

        redis.delete(key(postId));
    }
}