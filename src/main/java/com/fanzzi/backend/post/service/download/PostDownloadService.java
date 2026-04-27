package com.fanzzi.backend.post.service.download;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.media.gateway.channelpost.PostMediaGateway;
import com.fanzzi.backend.post.enums.MonetizationType;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.postUnlock.UnlockCacheService;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostDownloadService {

    private final ChannelPostRepository repository;
    private final UnlockCacheService unlockCacheService;
    private final PostMediaGateway mediaGateway;
    private final StringRedisTemplate redis;

    private static final String DOWNLOAD_COUNT_KEY = "post:downloads:";
    private static final String DOWNLOAD_USERS_KEY = "post:download:users:";
    private static final String ACTIVE_DOWNLOADS = "active:downloads";

    private static final Duration URL_TTL = Duration.ofMinutes(5);
    private static final Duration REDIS_TTL = Duration.ofHours(24);

    // ==========================================
    // 🔥 MAIN ENTRY
    // ==========================================
    public DownloadResponse getDownloadData(String postId) {

        if (postId == null || postId.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid postId");
        }

        ChannelPost post = repository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));

        validateAccess(post);

        String userId = SecurityUtil.getCurrentUserId();

        // ⚡ tracking (non-blocking style)
        trackDownload(postId, userId);

        List<String> urls = buildSignedUrls(post);

        if (urls.isEmpty()) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "Failed to generate download URLs");
        }

        log.debug("Download generated postId={} count={} at={}", postId, urls.size(), Instant.now());

        return DownloadResponse.builder()
                .downloadUrls(urls)
                .count(urls.size())
                .isMultiple(urls.size() > 1)
                .build();
    }

    // ==========================================
    // 🔒 VALIDATION
    // ==========================================
    private void validateAccess(ChannelPost post) {

        if (post.isDeleted()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        if (!post.isDownloadable()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Download not allowed");
        }

        String userId = SecurityUtil.getCurrentUserId();

        boolean isPaid = post.getMonetizationType() == MonetizationType.PAID;

        if (isPaid && !unlockCacheService.isUnlocked(userId, post.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Unlock required");
        }

        if (post.getAttachments() == null || post.getAttachments().isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "No downloadable content");
        }
    }

    // ==========================================
    // 📊 TRACKING (SAFE VERSION)
    // ==========================================
    private void trackDownload(String postId, String userId) {

        if (userId == null) return;

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

    // ==========================================
    // 🚀 URL GENERATION
    // ==========================================
    private List<String> buildSignedUrls(ChannelPost post) {

        return post.getAttachments()
                .stream()
                .map(att -> att.getKey())
                .filter(Objects::nonNull)
                .map(key -> {
                    try {
                        return mediaGateway.getSignedUrl(key, URL_TTL);
                    } catch (Exception e) {
                        log.warn("Failed to generate URL key={}", key, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}