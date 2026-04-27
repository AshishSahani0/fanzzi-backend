package com.fanzzi.backend.post.service.share;

import com.fanzzi.backend.post.repository.PostStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
@Service
public class PostShareFlushService {

    private final StringRedisTemplate redis;
    private final PostStatsRepository statsRepository;
    private final ApplicationEventPublisher publisher;

    private static final String SHARE_COUNT_KEY = "post:share:count:";
    private static final String ACTIVE_POSTS = "post:share:active";

    @Scheduled(fixedRate = 30000)
    public void flushShares() {

        var postIds = redis.opsForSet().members(ACTIVE_POSTS);
        if (postIds == null || postIds.isEmpty()) return;

        for (String postId : postIds) {

            try {
                String key = SHARE_COUNT_KEY + postId;

                // 🔥 atomic read + delete
                String value = redis.opsForValue().getAndDelete(key);
                if (value == null) continue;

                long delta;
                try {
                    delta = Long.parseLong(value);
                } catch (Exception e) {
                    log.warn("Invalid share count postId={} value={}", postId, value);
                    continue;
                }

                if (delta <= 0) continue;

                // ✅ correct DB update
                statsRepository.incrementShares(postId, delta);

                // =====================================
                // 🔥 FIXED TOTAL FETCH
                // =====================================
                Long totalValue = statsRepository.findSharesByPostId(postId);
                long total = totalValue != null ? totalValue : 0;

                // =====================================
                // 🚀 REALTIME EVENT
                // =====================================
                publisher.publishEvent(
                        new PostShareRealtimeEvent(postId, total)
                );

                // cleanup active set
                redis.opsForSet().remove(ACTIVE_POSTS, postId);

            } catch (Exception e) {
                log.warn("Share flush failed postId={}", postId, e);
            }
        }
    }
}