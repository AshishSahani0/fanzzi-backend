package com.fanzzi.backend.post.service.comments;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCommentEngagementService {

    private final StringRedisTemplate redis;

    private static final String LIKE_COUNT = "comment:likes:count:";
    private static final String LIKE_USERS = "comment:likes:users:";
    private static final String REPLY_COUNT = "comment:replies:count:";
    private static final String ACTIVE = "comment:active";

    private static final Duration TTL = Duration.ofHours(24);

    // =====================================
    // 🔥 TOGGLE LIKE (IMPROVED)
    // =====================================
    public boolean toggleLike(String commentId, String userId) {

        if (commentId == null || userId == null) return false;

        String userKey = LIKE_USERS + commentId;
        String countKey = LIKE_COUNT + commentId;

        try {

            // =====================================
            // CHECK MEMBERSHIP
            // =====================================
            Boolean exists = redis.opsForSet().isMember(userKey, userId);

            boolean liked;

            if (Boolean.TRUE.equals(exists)) {

                // ================= REMOVE LIKE
                Long removed = redis.opsForSet().remove(userKey, userId);

                if (removed != null && removed > 0) {
                    Long val = redis.opsForValue().decrement(countKey);

                    if (val != null && val < 0) {
                        redis.opsForValue().set(countKey, "0");
                    }
                }

                liked = false;

            } else {

                // ================= ADD LIKE
                Long added = redis.opsForSet().add(userKey, userId);

                if (added != null && added > 0) {
                    redis.opsForValue().increment(countKey);
                }

                liked = true;
            }

            track(commentId);

            return liked;

        } catch (Exception e) {
            log.warn("Like toggle failed commentId={} userId={}", commentId, userId, e);
            return false;
        }
    }

    // =====================================
    // 🔥 REPLY COUNT
    // =====================================
    public void incrementReplies(String commentId) {

        if (commentId == null) return;

        try {
            redis.opsForValue().increment(REPLY_COUNT + commentId);
            track(commentId);
        } catch (Exception e) {
            log.warn("Reply increment failed commentId={}", commentId, e);
        }
    }

    public void decrementReplies(String commentId) {

        if (commentId == null) return;

        try {
            Long val = redis.opsForValue().decrement(REPLY_COUNT + commentId);

            if (val != null && val < 0) {
                redis.opsForValue().set(REPLY_COUNT + commentId, "0");
            }

            track(commentId);

        } catch (Exception e) {
            log.warn("Reply decrement failed commentId={}", commentId, e);
        }
    }

    // =====================================
    // 📊 READ METHODS (IMPORTANT)
    // =====================================
    public long getLikeCount(String commentId) {

        try {
            String v = redis.opsForValue().get(LIKE_COUNT + commentId);
            return v != null ? Math.max(Long.parseLong(v), 0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public long getReplyCount(String commentId) {

        try {
            String v = redis.opsForValue().get(REPLY_COUNT + commentId);
            return v != null ? Math.max(Long.parseLong(v), 0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isLiked(String commentId, String userId) {

        try {
            return Boolean.TRUE.equals(
                    redis.opsForSet().isMember(LIKE_USERS + commentId, userId)
            );
        } catch (Exception e) {
            return false;
        }
    }

    // =====================================
    // 🔥 TRACK ACTIVE + TTL (FIXED)
    // =====================================
    private void track(String commentId) {

        try {
            redis.opsForSet().add(ACTIVE, commentId);

            redis.expire(LIKE_COUNT + commentId, TTL);
            redis.expire(REPLY_COUNT + commentId, TTL);
            redis.expire(LIKE_USERS + commentId, TTL);

            // 🔥 IMPORTANT: prevent memory leak
            redis.expire(ACTIVE, TTL);

        } catch (Exception e) {
            log.warn("Tracking failed commentId={}", commentId, e);
        }
    }
}