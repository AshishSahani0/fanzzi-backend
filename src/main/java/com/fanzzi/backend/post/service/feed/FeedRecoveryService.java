package com.fanzzi.backend.post.service.feed;

import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.service.mapping.PostResponseMapper;
import com.fanzzi.backend.post.postUnlock.UnlockCacheService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedRecoveryService {

    private static final int MAX_RECOVERY_BATCH = 50;

    private final ChannelPostRepository repository;
    private final PostResponseMapper mapper;
    private final HydratedFeedCacheService cacheService;
    private final UnlockCacheService unlockCacheService;

    public List<PostResponse> recover(String channelId, long lastSeenSeq) {

        if (channelId == null || lastSeenSeq < 0) {
            return List.of();
        }

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Feed recovery started channelId={} lastSeenSeq={}", channelId, lastSeenSeq);

        // =====================================
        // 1️⃣ FETCH FROM DB
        // =====================================
        List<ChannelPost> posts =
                repository.findPostsAfterSeq(
                        channelId,
                        lastSeenSeq,
                        Pageable.ofSize(MAX_RECOVERY_BATCH)
                );

        if (posts == null || posts.isEmpty()) {
            return List.of();
        }

        // =====================================
        // 🔄 MAP
        // =====================================
        List<PostResponse> mapped = posts.stream()
                .map(p -> mapper.map(p, 0, userId))
                .toList();

        // =====================================
        // 🚀 BULK UNLOCK CHECK (IMPORTANT)
        // =====================================
        enrichUnlockState(mapped, userId);

        // =====================================
        // 🔥 CACHE WARM
        // =====================================
        try {
            cacheService.warmCache(channelId, mapped);
        } catch (Exception e) {
            log.warn("Recovery cache warm failed channelId={}", channelId, e);
        }

        return mapped;
    }

    private List<PostResponse> enrichUnlockState(List<PostResponse> posts, String userId) {

        if (posts == null || posts.isEmpty()) return List.of();

        List<String> postIds = posts.stream()
                .map(PostResponse::getId)
                .toList();

        Set<String> unlocked =
                unlockCacheService.getUnlockedPostIds(userId, postIds);

        return posts.stream()
                .map(p -> PostResponse.builder()
                        .id(p.getId())
                        .seq(p.getSeq())
                        .channelId(p.getChannelId())
                        .ownerId(p.getOwnerId())
                        .text(p.getText())
                        .attachments(p.getAttachments())
                        .contentType(p.getContentType())
                        .monetizationType(p.getMonetizationType())
                        .price(p.getPrice())
                        .previewSeconds(p.getPreviewSeconds())
                        .unlocked(
                                userId.equals(p.getOwnerId()) ||
                                        p.getPrice() == 0 ||
                                        unlocked.contains(p.getId())
                        )
                        .edited(p.isEdited())
                        .pinned(p.isPinned())
                        .pinnedAt(p.getPinnedAt())
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .views(p.getViews())
                        .reactions(p.getReactions())
                        .comments(p.getComments())
                        .shares(p.getShares())
                        .downloadable(p.isDownloadable())
                        .canDownload(p.isCanDownload())
                        .hasMedia(p.isHasMedia())
                        .hasPoll(p.isHasPoll())
                        .isPaid(p.isPaid())
                        .poll(p.getPoll())
                        .build()
                )
                .toList();
    }
}