package com.fanzzi.backend.post.service.comments;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCommentCacheService {

    private final StringRedisTemplate redis;

    private static final String COMMENT_KEY = "post:comments:";
    private static final int MAX_CACHE_SIZE = 200;
    private static final int MAX_REQUEST_LIMIT = 50;

    private static final Duration TTL = Duration.ofHours(6);

    private String key(String postId) {
        return COMMENT_KEY + postId;
    }

    // =====================================
    // 🔥 CACHE SINGLE COMMENT
    // =====================================
    public void cacheComment(String postId, String commentJson) {

        if (postId == null || commentJson == null) return;

        try {
            String key = key(postId);

            redis.opsForList().leftPush(key, commentJson);
            redis.opsForList().trim(key, 0, MAX_CACHE_SIZE - 1);
            redis.expire(key, TTL);

        } catch (Exception e) {
            log.warn("Comment cache failed postId={}", postId, e);
        }
    }

    // =====================================
    // 🔥 BULK CACHE (IMPORTANT)
    // =====================================
    public void cacheComments(String postId, List<String> commentsJson) {

        if (postId == null || commentsJson == null || commentsJson.isEmpty()) return;

        try {
            String key = key(postId);

            redis.delete(key);

            redis.opsForList().rightPushAll(key, commentsJson);

            redis.opsForList().trim(key, 0, MAX_CACHE_SIZE - 1);

            redis.expire(key, TTL);

        } catch (Exception e) {
            log.warn("Bulk comment cache failed postId={}", postId, e);
        }
    }

    // =====================================
    // ⚡ FETCH COMMENTS
    // =====================================
    public List<String> getCachedComments(String postId, int limit) {

        if (postId == null || limit <= 0) {
            return Collections.emptyList();
        }

        limit = Math.min(limit, MAX_REQUEST_LIMIT);

        try {
            List<String> result =
                    redis.opsForList().range(key(postId), 0, limit - 1);

            return result != null ? result : Collections.emptyList();

        } catch (Exception e) {
            log.warn("Comment cache read failed postId={}", postId, e);
            return Collections.emptyList();
        }
    }

    // =====================================
    // 🔄 UPDATE COMMENT (REPLACE)
    // =====================================
    public void updateComment(String postId, String commentId, String updatedJson) {

        if (postId == null || commentId == null || updatedJson == null) return;

        try {
            String key = key(postId);

            List<String> list = redis.opsForList().range(key, 0, -1);
            if (list == null || list.isEmpty()) return;

            List<String> updated = list.stream()
                    .map(json -> json.contains("\"id\":\"" + commentId + "\"")
                            ? updatedJson
                            : json
                    )
                    .toList();

            redis.delete(key);
            redis.opsForList().rightPushAll(key, updated);
            redis.expire(key, TTL);

        } catch (Exception e) {
            log.warn("Comment cache update failed postId={} commentId={}",
                    postId, commentId, e);
        }
    }

    // =====================================
    // ❌ REMOVE COMMENT
    // =====================================
    public void removeComment(String postId, String commentJson) {

        if (postId == null || commentJson == null) return;

        try {
            redis.opsForList().remove(key(postId), 1, commentJson);
        } catch (Exception e) {
            log.warn("Comment cache remove failed postId={}", postId, e);
        }
    }

    // =====================================
    // 🔥 CLEAR CACHE
    // =====================================
    public void clear(String postId) {

        if (postId == null) return;

        try {
            redis.delete(key(postId));
        } catch (Exception e) {
            log.warn("Comment cache clear failed postId={}", postId, e);
        }
    }
}