package com.fanzzi.backend.post.service.download;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostDownloadTrackingService {

    private final StringRedisTemplate redis;

    private static final String DOWNLOAD_COUNT_KEY = "post:downloads:";
    private static final String DOWNLOAD_USERS_KEY = "post:download:users:";
    private static final String ACTIVE_DOWNLOADS = "active:downloads";

    private static final Duration REDIS_TTL = Duration.ofHours(24);

    @Async
    public void track(String postId, String userId) {

        if (postId == null || userId == null) return;

        try {
            String userKey = DOWNLOAD_USERS_KEY + postId;

            Long added = redis.opsForSet().add(userKey, userId);

            if (added != null && added > 0) {
                redis.opsForValue().increment(DOWNLOAD_COUNT_KEY + postId);
            }

            redis.opsForSet().add(ACTIVE_DOWNLOADS, postId);

            redis.expire(userKey, REDIS_TTL);
            redis.expire(DOWNLOAD_COUNT_KEY + postId, REDIS_TTL);

        } catch (Exception e) {
            log.warn("Download tracking failed postId={} userId={}", postId, userId, e);
        }
    }
}
