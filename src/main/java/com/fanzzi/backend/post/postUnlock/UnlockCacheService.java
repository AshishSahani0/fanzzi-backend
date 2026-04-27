package com.fanzzi.backend.post.postUnlock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnlockCacheService {

    private final StringRedisTemplate redis;

    private static final String KEY = "unlock:%s:%s";
    private static final Duration TTL = Duration.ofDays(30);

    private String key(String userId, String postId) {
        return KEY.formatted(userId, postId);
    }

    // =====================================
    // ✅ MARK UNLOCKED
    // =====================================
    public void markUnlocked(String userId, String postId) {

        if (userId == null || postId == null) return;

        try {
            redis.opsForValue().set(key(userId, postId), "1", TTL);
        } catch (Exception e) {
            log.warn("Unlock cache write failed userId={} postId={}", userId, postId, e);
        }
    }

    // =====================================
    // ⚡ FAST CHECK
    // =====================================
    public boolean isUnlocked(String userId, String postId) {

        if (userId == null || postId == null) return false;

        try {
            return redis.opsForValue().get(key(userId, postId)) != null;
        } catch (Exception e) {
            log.warn("Unlock cache read failed userId={} postId={}", userId, postId, e);
            return false;
        }
    }

    // =====================================
    // 🚀 BULK CHECK (FEED OPTIMIZATION)
    // =====================================
    public Set<String> getUnlockedPostIds(String userId, List<String> postIds) {

        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }

        try {
            List<String> keys = new ArrayList<>(postIds.size());

            for (String postId : postIds) {
                if (postId != null) {
                    keys.add(key(userId, postId));
                }
            }

            if (keys.isEmpty()) {
                return Collections.emptySet();
            }

            List<String> values = redis.opsForValue().multiGet(keys);

            Set<String> unlocked = new HashSet<>();

            if (values == null) return unlocked;

            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) != null) {
                    unlocked.add(postIds.get(i));
                }
            }

            return unlocked;

        } catch (Exception e) {
            log.warn("Bulk unlock cache failed userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    // =====================================
    // ❌ REMOVE (REFUND / REVOKE)
    // =====================================
    public void removeUnlocked(String userId, String postId) {

        if (userId == null || postId == null) return;

        try {
            redis.delete(key(userId, postId));
        } catch (Exception e) {
            log.warn("Unlock cache delete failed userId={} postId={}", userId, postId, e);
        }
    }
}