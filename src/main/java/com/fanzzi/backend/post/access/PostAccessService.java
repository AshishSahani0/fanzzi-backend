package com.fanzzi.backend.post.access;

import com.fanzzi.backend.channel.access.dto.ChannelAccess;
import com.fanzzi.backend.channel.access.service.ChannelAccessService;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.enums.PostStatus;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.postUnlock.*;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.repository.PostStatsRepository;
import com.fanzzi.backend.post.service.feed.HydratedFeedCacheService;
import com.fanzzi.backend.post.service.view.RedisPostViewCounterService;
import com.fanzzi.backend.wallets.stars.monetization.ContentPurchaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostAccessService {

    private final ChannelPostRepository postRepo;
    private final PostUnlockRepository unlockRepo;
    private final ChannelAccessService channelAccessService;
    private final ContentPurchaseService purchaseService;
    private final PostStatsRepository statsRepo;
    private final UnlockCacheService unlockCacheService;
    private final RedisPostViewCounterService viewCounterService;
    private final ChannelRepository channelRepository;
    private final HydratedFeedCacheService cacheService;

    // 🔥 NEW (REALTIME)
    private final ApplicationEventPublisher publisher;

    // =====================================
    // 🔒 SAFE LOCK (SINGLE BACKEND)
    // =====================================
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    private Object getLock(String postId, String userId) {
        String key = postId + ":" + userId;
        return locks.computeIfAbsent(key, k -> new Object());
    }

    // =====================================
    // 🔥 UNLOCK POST
    // =====================================
    public UnlockResponse unlockPost(String postId, String userId) {

        ChannelPost post = loadPost(postId);

        ChannelAccess access =
                channelAccessService.resolve(post.getChannelId(), userId);

        if (!access.canRead()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "No channel access");
        }

        // 👑 OWNER
        if (isOwner(post, userId)) {
            return new UnlockResponse(
                    postId, true, 0, false, true, null, null
            );
        }

        // 🆓 FREE
        if (post.getPrice() <= 0) {
            return new UnlockResponse(
                    postId, true, 0, false, false, null, null
            );
        }

        // ⚡ REDIS
        if (unlockCacheService.isUnlocked(userId, postId)) {
            return new UnlockResponse(
                    postId, true, post.getPrice(), true, false, null, null
            );
        }

        // 🧠 DB
        PostUnlock existing =
                unlockRepo.findActiveByUserIdAndPostId(userId, postId)
                        .orElse(null);

        if (existing != null) {
            unlockCacheService.markUnlocked(userId, postId);

            return new UnlockResponse(
                    postId,
                    true,
                    existing.getPricePaid(),
                    true,
                    false,
                    existing.getUnlockedAt(),
                    existing.getTransactionId()
            );
        }

        // =====================================
        // 🔒 LOCK (DOUBLE CHECK)
        // =====================================
        synchronized (getLock(postId, userId)) {

            PostUnlock again =
                    unlockRepo.findActiveByUserIdAndPostId(userId, postId)
                            .orElse(null);

            if (again != null) {
                unlockCacheService.markUnlocked(userId, postId);

                return new UnlockResponse(
                        postId,
                        true,
                        again.getPricePaid(),
                        true,
                        false,
                        again.getUnlockedAt(),
                        again.getTransactionId()
                );
            }

            // =====================================
            // 💰 PURCHASE
            // =====================================
            String txnId = purchaseService.purchase(
                    userId,
                    post.getPostedByUserId(),
                    post.getChannelId(),
                    postId,
                    post.getPrice()
            );

            Instant now = Instant.now();

            PostUnlock unlock = PostUnlock.builder()
                    .userId(userId)
                    .postId(postId)
                    .channelId(post.getChannelId())
                    .pricePaid(post.getPrice())
                    .transactionId(txnId)
                    .unlockedAt(now)
                    .build();

            unlockRepo.save(unlock);

            // ✅ CACHE
            // ✅ REDIS
            unlockCacheService.markUnlocked(userId, postId);


            try {
                cacheService.markPostUnlocked(post.getChannelId(), postId);
            } catch (Exception e) {
                log.warn("Feed cache unlock update failed postId={}", postId, e);
            }


            try {
                statsRepo.incrementUnlocks(postId, now);
            } catch (Exception e) {
                log.warn("Unlock stats failed postId={}", postId, e);
            }

            try {
                publisher.publishEvent(
                        new UnlockRealtimeEvent(postId, userId)
                );
            } catch (Exception e) {
                log.warn("Unlock realtime event failed postId={}", postId, e);
            }

            return new UnlockResponse(
                    postId,
                    true,
                    post.getPrice(),
                    true,
                    false,
                    now,
                    txnId
            );
        }
    }

    // =====================================
    // 👑 OWNER CHECK
    // =====================================
    private boolean isOwner(ChannelPost post, String userId) {

        if (userId.equals(post.getPostedByUserId())) {
            return true;
        }

        return channelRepository.findById(post.getChannelId())
                .map(c -> userId.equals(c.getOwnerId()))
                .orElse(false);
    }

    // =====================================
    // 👁️ RECORD VIEW
    // =====================================
    public void recordView(String postId, String userId) {

        ChannelPost post = loadPost(postId);

        ChannelAccess access =
                channelAccessService.resolve(post.getChannelId(), userId);

        if (!access.canRead()) return;

        String key = "post:view:user:" + postId + ":" + userId;

        if (viewCounterService.tryUniqueView(key)) {
            viewCounterService.incrementView(postId, userId);
        }
    }

    // =====================================
    // HELPERS
    // =====================================
    private ChannelPost loadPost(String postId) {

        ChannelPost post = postRepo.findById(postId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.POST_NOT_FOUND));

        validatePost(post);

        return post;
    }

    private void validatePost(ChannelPost post) {

        if (post.isDeleted()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Post not available");
        }
    }
}