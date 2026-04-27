package com.fanzzi.backend.post.service.post;

import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.service.mapping.PostResponseMapper;
import com.fanzzi.backend.post.postUnlock.UnlockCacheService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPostService {

    private final ChannelPostRepository repository;
    private final PostResponseMapper mapper;
    private final UnlockCacheService unlockCacheService;
    private final PostCacheService cacheService;

    public PostResponse getPost(String postId, String userId) {

        // =====================================
        // 1️⃣ CACHE FIRST
        // =====================================
        PostResponse cached = cacheService.get(postId);

        if (cached != null) {
            return enrichUnlock(cached, userId);
        }

        // =====================================
        // 2️⃣ DB FALLBACK
        // =====================================
        ChannelPost post = repository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        boolean isOwner = userId.equals(post.getPostedByUserId());
        boolean isPaid = post.getPrice() > 0;

        boolean unlockedFromCache =
                unlockCacheService.isUnlocked(userId, postId);

        boolean unlocked =
                isOwner || !isPaid || unlockedFromCache;

        PostResponse response =
                mapper.map(post, 0, unlocked, userId);

        // =====================================
        // 3️⃣ CACHE STORE
        // =====================================
        cacheService.put(response);

        return response;
    }

    // =====================================
    // 🔓 PER-USER ENRICH (IMPORTANT)
    // =====================================
    private PostResponse enrichUnlock(PostResponse p, String userId) {

        boolean isOwner = userId.equals(p.getOwnerId());
        boolean isPaid = p.getPrice() > 0;

        boolean unlockedFromCache =
                unlockCacheService.isUnlocked(userId, p.getId());

        boolean unlocked =
                isOwner || !isPaid || unlockedFromCache;

        // ⚠️ DO NOT mutate cached object
        return PostResponse.builder()
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
                .unlocked(unlocked)
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
                .build();
    }
}