package com.fanzzi.backend.post.service.view;

import com.fanzzi.backend.post.repository.PostStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostViewFlushService {

    private final StringRedisTemplate redis;
    private final PostStatsRepository statsRepository;

    private static final String VIEW_COUNT_KEY = "post:view:count:";
    private static final String ACTIVE_POSTS = "post:view:active";

    @Scheduled(fixedRate = 30000)
    public void flushViews() {

        Set<String> postIds = redis.opsForSet().members(ACTIVE_POSTS);

        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        for (String postId : postIds) {

            try {
                String key = VIEW_COUNT_KEY + postId;

                // 🔥 ATOMIC FETCH + DELETE
                String value = redis.opsForValue().getAndDelete(key);

                if (value == null) continue;

                long views;

                try {
                    views = Long.parseLong(value);
                } catch (Exception e) {
                    log.warn("Invalid view count postId={} value={}", postId, value);
                    continue;
                }

                if (views <= 0) continue;

                statsRepository.incrementViewsBy(postId, views, Instant.now());

                // 🔥 CLEAN ACTIVE SET (if no more views)
                redis.opsForSet().remove(ACTIVE_POSTS, postId);

            } catch (Exception e) {
                log.warn("Flush failed postId={}", postId, e);
            }
        }
    }
}