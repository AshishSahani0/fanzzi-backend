package com.fanzzi.backend.post.service.feed;

import com.fanzzi.backend.post.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HydratedFeedCacheService {

    private final RedisTemplate<String, Object> redis;

    private static final String KEY_PATTERN = "channel:%s:hydrated_feed";

    private static final int MAX_CACHE_SIZE = 50;
    private static final int MAX_REQUEST_LIMIT = 50;

    private static final Duration TTL = Duration.ofHours(6);

    private String key(String channelId) {
        return KEY_PATTERN.formatted(channelId);
    }

    // ================================
    // 🔥 CACHE SINGLE POST
    // ================================
    public void cachePost(String channelId, PostResponse post) {

        if (channelId == null || post == null) return;

        try {
            String key = key(channelId);

            redis.opsForList().leftPush(key, post);
            redis.opsForList().trim(key, 0, MAX_CACHE_SIZE - 1);
            redis.expire(key, TTL);

        } catch (Exception e) {
            log.warn("Cache post failed channelId={}", channelId, e);
        }
    }

    // ================================
    // ⚡ GET FEED
    // ================================
    public List<PostResponse> getFeed(String channelId, int limit) {

        if (channelId == null || limit <= 0) return List.of();

        limit = Math.min(limit, MAX_REQUEST_LIMIT);

        try {
            List<Object> raw =
                    redis.opsForList().range(key(channelId), 0, limit - 1);

            if (raw == null || raw.isEmpty()) return List.of();

            return raw.stream()
                    .filter(PostResponse.class::isInstance)
                    .map(PostResponse.class::cast)
                    .toList();

        } catch (Exception e) {
            log.warn("Feed cache read failed channelId={}", channelId, e);
            return List.of();
        }
    }

    // ================================
    // 🔥 WARM CACHE (FIXED)
    // ================================
    public void warmCache(String channelId, List<PostResponse> posts) {

        if (channelId == null || posts == null || posts.isEmpty()) return;

        try {
            String key = key(channelId);

            redis.delete(key);

            // ✅ FIX: use list directly
            redis.opsForList().rightPushAll(key, posts);

            redis.opsForList().trim(key, 0, MAX_CACHE_SIZE - 1);
            redis.expire(key, TTL);

        } catch (Exception e) {
            log.warn("Feed cache warm failed channelId={}", channelId, e);
        }
    }

    // ================================
    // ❌ CLEAR CACHE
    // ================================
    public void clear(String channelId) {
        try {
            redis.delete(key(channelId));
        } catch (Exception e) {
            log.warn("Cache clear failed channelId={}", channelId, e);
        }
    }

    // ================================
    // 🔄 UPDATE SINGLE POST
    // ================================
    public void updatePost(String channelId, PostResponse updatedPost) {

        if (channelId == null || updatedPost == null) return;

        try {
            String key = key(channelId);

            List<Object> raw = redis.opsForList().range(key, 0, -1);

            if (raw == null || raw.isEmpty()) return;

            List<PostResponse> updated = raw.stream()
                    .filter(PostResponse.class::isInstance)
                    .map(PostResponse.class::cast)
                    .map(p -> p.getId().equals(updatedPost.getId()) ? updatedPost : p)
                    .collect(Collectors.toList());

            redis.delete(key);
            redis.opsForList().rightPushAll(key, updated);
            redis.expire(key, TTL);

        } catch (Exception e) {
            log.warn("Cache update failed channelId={}", channelId, e);
        }
    }

    // ================================
    // ❌ REMOVE SINGLE POST
    // ================================
    public void removePost(String channelId, String postId) {

        if (channelId == null || postId == null) return;

        try {
            String key = key(channelId);

            List<Object> raw = redis.opsForList().range(key, 0, -1);

            if (raw == null || raw.isEmpty()) return;

            List<PostResponse> updated = raw.stream()
                    .filter(PostResponse.class::isInstance)
                    .map(PostResponse.class::cast)
                    .filter(p -> !p.getId().equals(postId))
                    .toList();

            redis.delete(key);

            if (!updated.isEmpty()) {
                redis.opsForList().rightPushAll(key, updated);
                redis.expire(key, TTL);
            }

        } catch (Exception e) {
            log.warn("Cache remove failed channelId={}", channelId, e);
        }
    }

    // ================================
    // ❌ REMOVE MULTIPLE POSTS
    // ================================
    public void removePosts(String channelId, Set<String> postIds) {

        if (channelId == null || postIds == null || postIds.isEmpty()) return;

        try {
            String key = key(channelId);

            List<Object> raw = redis.opsForList().range(key, 0, -1);

            if (raw == null || raw.isEmpty()) return;

            List<PostResponse> updated = raw.stream()
                    .filter(PostResponse.class::isInstance)
                    .map(PostResponse.class::cast)
                    .filter(p -> !postIds.contains(p.getId()))
                    .toList();

            redis.delete(key);

            if (!updated.isEmpty()) {
                redis.opsForList().rightPushAll(key, updated);
                redis.expire(key, TTL);
            }

        } catch (Exception e) {
            log.warn("Bulk remove failed channelId={}", channelId, e);
        }
    }

    // ================================
    // 🚀🔥 NEW: UPDATE UNLOCK STATE
    // ================================
    public void markPostUnlocked(String channelId, String postId) {

        if (channelId == null || postId == null) return;

        try {
            String key = key(channelId);

            List<Object> raw = redis.opsForList().range(key, 0, -1);

            if (raw == null || raw.isEmpty()) return;

            List<PostResponse> updated = raw.stream()
                    .filter(PostResponse.class::isInstance)
                    .map(PostResponse.class::cast)
                    .map(p -> {
                        if (p.getId().equals(postId)) {
                            p.setUnlocked(true); // 🔥 KEY FEATURE
                        }
                        return p;
                    })
                    .toList();

            redis.delete(key);
            redis.opsForList().rightPushAll(key, updated);
            redis.expire(key, TTL);

        } catch (Exception e) {
            log.warn("Unlock cache update failed channelId={} postId={}", channelId, postId, e);
        }
    }
}