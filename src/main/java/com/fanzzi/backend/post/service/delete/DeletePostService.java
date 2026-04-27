package com.fanzzi.backend.post.service.delete;

import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.service.feed.HydratedFeedCacheService;
import com.fanzzi.backend.post.service.validation.PostValidationService;
import com.fanzzi.backend.post.util.PostDeleteRealtimeEvent;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletePostService {

    private final ChannelPostRepository repository;
    private final ChannelRepository channelRepository;

    private final PostValidationService validationService;

    private final HydratedFeedCacheService cacheService;
    private final ApplicationEventPublisher publisher;

    /**
     * ==========================================================
     * DELETE POST
     * ==========================================================
     *
     * Workflow:
     *
     * 1️⃣ Validate post exists
     * 2️⃣ Validate channel ownership / permissions
     * 3️⃣ Soft delete in Mongo
     * 4️⃣ Remove from Redis feed cache
     * 5️⃣ Update channel post count
     *
     * Transaction ensures consistency between Mongo operations.
     */
    @Transactional
    public void deletePost(String channelId, String postId) {

        ChannelPost post = repository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));

        if (!post.getChannelId().equals(channelId)) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        String userId = SecurityUtil.getCurrentUserId();

        validationService.validateDelete(post, channelId, userId);

        // 🔥 Prevent double delete
        if (post.isDeleted()) {
            return;
        }

        Instant now = Instant.now();

        // ======================================
        // DB UPDATE
        // ======================================
        repository.softDeletePost(postId, now);

        // ======================================
        // CACHE
        // ======================================
        try {
            cacheService.removePost(channelId, postId);
        } catch (Exception e) {
            log.warn("Feed cache remove failed channelId={} postId={}", channelId, postId, e);
        }

        // ======================================
        // CHANNEL COUNT
        // ======================================
        try {
            channelRepository.decrementPostCount(channelId);
        } catch (Exception e) {
            log.error("Failed to decrement post count for channel {}", channelId, e);
        }

        // ======================================
        // REALTIME (AFTER COMMIT IDEAL)
        // ======================================
        publisher.publishEvent(
                new PostDeleteRealtimeEvent(
                        channelId,
                        postId,
                        post.getSeq()
                )
        );
    }
}
