package com.fanzzi.backend.post.service.poll;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PollSyncScheduler {

    private final StringRedisTemplate redis;
    private final PollSyncService syncService;

    private static final String ACTIVE_POLLS = "poll:active";
    private static final String LOCK_KEY = "poll:sync:lock";

    private static final int MAX_BATCH = 50;
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    // =====================================
    // 🔥 SYNC POLLS (DISTRIBUTED SAFE)
    // =====================================
    @Scheduled(fixedDelay = 5000)
    public void syncAllPolls() {

        // =====================================
        // 🔒 DISTRIBUTED LOCK (IMPORTANT)
        // =====================================
        Boolean locked = redis.opsForValue()
                .setIfAbsent(LOCK_KEY, "1", LOCK_TTL);

        if (!Boolean.TRUE.equals(locked)) {
            return; // another instance is working
        }

        try {
            Set<String> postIds = redis.opsForSet().members(ACTIVE_POLLS);

            if (postIds == null || postIds.isEmpty()) {
                return;
            }

            int processed = 0;

            for (String postId : postIds) {

                if (processed >= MAX_BATCH) break;

                try {
                    syncService.syncPoll(postId);

                    // ✅ remove ONLY after success
                    redis.opsForSet().remove(ACTIVE_POLLS, postId);

                    processed++;

                } catch (Exception e) {
                    log.warn("Poll sync failed postId={}", postId, e);
                }
            }

            log.debug("Poll sync batch processed count={}", processed);

        } finally {
            // 🔓 release lock
            redis.delete(LOCK_KEY);
        }
    }
}