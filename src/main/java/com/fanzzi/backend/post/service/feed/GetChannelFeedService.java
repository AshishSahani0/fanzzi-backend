package com.fanzzi.backend.post.service.feed;

import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.service.mapping.PostResponseMapper;
import com.fanzzi.backend.post.util.FeedBucketUtil;
import com.fanzzi.backend.post.postUnlock.UnlockCacheService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class GetChannelFeedService {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final ChannelPostRepository repository;
    private final PostResponseMapper mapper;
    private final HydratedFeedCacheService cacheService;
    private final UnlockCacheService unlockCacheService;

    public List<PostResponse> execute(String channelId, Long beforeSeq) {

        String userId = SecurityUtil.getCurrentUserId();

        // =====================================
        // 🛡️ SAFE CURSOR
        // =====================================
        final Long safeBeforeSeq =
                (beforeSeq != null && beforeSeq > 0) ? beforeSeq : null;

        // =====================================
        // 1️⃣ REDIS FIRST
        // =====================================
        int fetchSize = safeBeforeSeq == null ? PAGE_SIZE : PAGE_SIZE * 3;

        List<PostResponse> cached =
                cacheService.getFeed(channelId, fetchSize);

        if (!cached.isEmpty()) {

            List<PostResponse> filtered = cached.stream()
                    .filter(p -> safeBeforeSeq == null || p.getSeq() < safeBeforeSeq)
                    .limit(PAGE_SIZE)
                    .toList();

            if (!filtered.isEmpty()) {
                log.debug("Feed served from cache channelId={} size={}", channelId, filtered.size());

                // 🔥 enrich unlock state (IMPORTANT)
                enrichUnlockState(filtered, userId);

                return filtered;
            }
        }

        // =====================================
        // 2️⃣ DB FALLBACK (BUCKET-AWARE)
        // =====================================
        log.debug("Cache miss → DB load channelId={} beforeSeq={}", channelId, safeBeforeSeq);

        int safePageSize = Math.min(PAGE_SIZE, MAX_PAGE_SIZE);

        List<ChannelPost> result = new ArrayList<>();

        long currentBucket =
                FeedBucketUtil.calculateBucket(
                        safeBeforeSeq != null ? safeBeforeSeq : Long.MAX_VALUE
                );

        while (result.size() < PAGE_SIZE && currentBucket >= 0) {

            List<ChannelPost> batch;

            if (safeBeforeSeq == null) {
                batch = repository.findByChannelIdAndBucketIdAndDeletedFalseOrderBySeqDesc(
                        channelId,
                        (int) currentBucket,
                        PageRequest.of(0, safePageSize)
                );
            } else {
                batch = repository.findByChannelIdAndBucketIdAndSeqLessThanAndDeletedFalseOrderBySeqDesc(
                        channelId,
                        (int) currentBucket,
                        safeBeforeSeq,
                        PageRequest.of(0, safePageSize)
                );
            }

            if (!batch.isEmpty()) {
                result.addAll(batch);
            }

            currentBucket = FeedBucketUtil.previousBucket(currentBucket);
            beforeSeq = null;
        }

        if (result.isEmpty()) {
            return List.of();
        }

        // =====================================
        // 🔄 MAP RESPONSE
        // =====================================
        List<PostResponse> mapped = result.stream()
                .limit(PAGE_SIZE)
                .map(p -> mapper.map(p, 0, userId))
                .toList();

        // 🔥 enrich unlock state (IMPORTANT)
        enrichUnlockState(mapped, userId);

        // =====================================
        // 🔥 CACHE WARM
        // =====================================
        try {
            cacheService.warmCache(channelId, mapped);
        } catch (Exception e) {
            log.warn("Feed cache warm failed channelId={}", channelId, e);
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