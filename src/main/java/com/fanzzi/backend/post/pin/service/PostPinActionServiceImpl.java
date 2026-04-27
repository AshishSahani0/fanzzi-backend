package com.fanzzi.backend.post.pin.service;

import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.enums.PostStatus;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostPinActionServiceImpl implements PostPinActionService {

    private final ChannelPostRepository postRepository;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher publisher;
    private final ChannelRepository channelRepository;

    private static final String CACHE_KEY = "channel:pinned:";
    private static final int MAX_PINNED = 5;

    @Override
    public boolean pinPost(String channelId, String postId) {

        ChannelPost post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.POST_NOT_FOUND));

        validatePost(channelId, post);
        validatePermission(post);

        // =========================
        // IDEMPOTENT CHECK
        // =========================
        if (Boolean.TRUE.equals(post.isPinned())) {
            return false;
        }

        // =========================
        // LIMIT PIN COUNT
        // =========================
        List<ChannelPost> pinned =
                postRepository.findByChannelIdAndDeletedFalseAndPinnedTrueOrderByPinnedAtDesc(channelId);

        if (pinned.size() >= MAX_PINNED) {

            // remove oldest pin
            ChannelPost oldest = pinned.get(pinned.size() - 1);

            oldest.setPinned(false);
            oldest.setPinnedAt(null);

            postRepository.save(oldest);
        }

        // =========================
        // PIN POST
        // =========================
        post.setPinned(true);
        post.setPinnedAt(Instant.now());

        postRepository.save(post);

        // =========================
        // CACHE INVALIDATION
        // =========================
        redis.delete(CACHE_KEY + channelId);

        // =========================
        // REALTIME EVENT
        // =========================
        publisher.publishEvent(
                new PostPinEvent(channelId, postId, true)
        );

        return true;
    }

    @Override
    public boolean unpinPost(String channelId, String postId) {

        ChannelPost post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.POST_NOT_FOUND));

        validatePost(channelId, post);
        validatePermission(post);

        // =========================
        // IDEMPOTENT
        // =========================
        if (!Boolean.TRUE.equals(post.isPinned())) {
            return false;
        }

        post.setPinned(false);
        post.setPinnedAt(null);

        postRepository.save(post);

        // =========================
        // CACHE INVALIDATION
        // =========================
        redis.delete(CACHE_KEY + channelId);

        // =========================
        // REALTIME EVENT
        // =========================
        publisher.publishEvent(
                new PostPinEvent(channelId, postId, false)
        );

        return true;
    }

    // =====================================
    // 🔒 VALIDATION
    // =====================================
    private void validatePost(String channelId, ChannelPost post) {

        if (!post.getChannelId().equals(channelId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid channel");
        }

        if (post.isDeleted()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Post not published");
        }
    }

    private void validatePermission(ChannelPost post) {

        String userId = SecurityUtil.getCurrentUserId();

        var channel = channelRepository.findById(post.getChannelId())
                .orElseThrow(() ->
                        new ApiException(ErrorCode.CHANNEL_NOT_FOUND)
                );

        // ✅ ONLY OWNER FOR NOW
        if (!userId.equals(channel.getOwnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not allowed");
        }
    }
}