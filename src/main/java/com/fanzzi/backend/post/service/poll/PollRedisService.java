package com.fanzzi.backend.post.service.poll;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollRedisService {

    private final StringRedisTemplate redis;

    private static final String POLL_KEY = "poll:";
    private static final String ACTIVE_POLLS = "poll:active";

    private static final Duration TTL = Duration.ofHours(24);

    // =====================================
    // ➕ INCREMENT
    // =====================================
    public void increment(String postId, String optionId) {

        if (postId == null || optionId == null) return;

        try {
            redis.opsForHash().increment(key(postId), optionId, 1);
            track(postId);
        } catch (Exception e) {
            log.warn("Poll increment failed postId={} optionId={}", postId, optionId, e);
        }
    }

    // =====================================
    // ➖ DECREMENT (ATOMIC SAFE)
    // =====================================
    public void decrement(String postId, String optionId) {

        if (postId == null || optionId == null) return;

        try {
            String redisKey = key(postId);

            Long value = redis.opsForHash().increment(redisKey, optionId, -1);

            // 🔥 hard guard (atomic correction)
            if (value != null && value < 0) {
                redis.opsForHash().put(redisKey, optionId, "0");
            }

            track(postId);

        } catch (Exception e) {
            log.warn("Poll decrement failed postId={} optionId={}", postId, optionId, e);
        }
    }

    // =====================================
    // 📊 GET STATS (TYPE SAFE)
    // =====================================
    public Map<String, Long> getStats(String postId) {

        if (postId == null) return Collections.emptyMap();

        try {
            Map<Object, Object> raw =
                    redis.opsForHash().entries(key(postId));

            if (raw == null || raw.isEmpty()) return Collections.emptyMap();

            Map<String, Long> result = new HashMap<>();

            for (Map.Entry<Object, Object> entry : raw.entrySet()) {
                try {
                    String optionId = entry.getKey().toString();
                    long votes = Long.parseLong(entry.getValue().toString());

                    if (votes < 0) votes = 0;

                    result.put(optionId, votes);
                } catch (Exception e) {
                    log.warn("Invalid poll stat value postId={} key={}", postId, entry.getKey());
                }
            }

            return result;

        } catch (Exception e) {
            log.warn("Poll stats fetch failed postId={}", postId, e);
            return Collections.emptyMap();
        }
    }

    // =====================================
    // 🚀 BULK FETCH (PERF BOOST)
    // =====================================
    public Map<String, Map<String, Long>> getStatsBulk(Iterable<String> postIds) {

        Map<String, Map<String, Long>> result = new HashMap<>();

        if (postIds == null) return result;

        for (String postId : postIds) {
            result.put(postId, getStats(postId));
        }

        return result;
    }

    // =====================================
    // 🧹 CLEAR POLL CACHE
    // =====================================
    public void clear(String postId) {
        redis.delete(key(postId));
        redis.opsForSet().remove(ACTIVE_POLLS, postId);
    }

    // =====================================
    // 🔥 TRACK ACTIVE POLLS
    // =====================================
    private void track(String postId) {

        try {
            redis.opsForSet().add(ACTIVE_POLLS, postId);

            redis.expire(key(postId), TTL);
            redis.expire(ACTIVE_POLLS, TTL);

        } catch (Exception e) {
            log.warn("Poll tracking failed postId={}", postId, e);
        }
    }

    private String key(String postId) {
        return POLL_KEY + postId;
    }
}