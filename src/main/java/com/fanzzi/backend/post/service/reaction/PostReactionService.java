package com.fanzzi.backend.post.service.reaction;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.enums.ReactionType;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.model.PostReaction;
import com.fanzzi.backend.post.model.PostStats;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.repository.PostReactionRepository;
import com.fanzzi.backend.post.repository.PostStatsRepository;
import com.fanzzi.backend.post.service.stats.PostStatsUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostReactionService {

    private final PostReactionRepository reactionRepo;
    private final PostStatsRepository statsRepo;
    private final ChannelPostRepository postRepository;
    private final PostStatsUpdateService statsUpdateService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public PostReactionResponse react(String postId, String userId, ReactionType reaction) {

        if (postId == null || postId.isBlank()
                || userId == null || userId.isBlank()
                || reaction == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid reaction input");
        }

        ChannelPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        Instant now = Instant.now();
        ReactionType finalUserReaction = null;

        try {
            PostReaction existing =
                    reactionRepo.findByPostIdAndUserId(postId, userId)
                            .orElse(null);

            // 1️⃣ ADD
            if (existing == null) {

                PostReaction r = new PostReaction();
                r.setPostId(postId);
                r.setUserId(userId);
                r.setReaction(reaction);
                r.setCreatedAt(now);

                reactionRepo.save(r);

                statsUpdateService.incrementReaction(postId, reaction.name(), now);

                finalUserReaction = reaction;
            }

            // 2️⃣ TOGGLE (REMOVE)
            else if (existing.getReaction() == reaction) {

                reactionRepo.delete(existing);

                statsUpdateService.decrementReaction(postId, reaction.name(), now);

                finalUserReaction = null;
            }

            // 3️⃣ UPDATE
            else {

                ReactionType old = existing.getReaction();

                existing.setReaction(reaction);
                existing.setUpdatedAt(now);

                reactionRepo.save(existing);

                statsUpdateService.decrementReaction(postId, old.name(), now);
                statsUpdateService.incrementReaction(postId, reaction.name(), now);

                finalUserReaction = reaction;
            }

        } catch (DuplicateKeyException e) {
            log.warn("Duplicate reaction prevented postId={} userId={}", postId, userId);
            finalUserReaction = reaction;
        }

        // =====================================
        // ✅ FIXED: USE TOTAL ONLY (LONG)
        // =====================================
        PostStats stats = statsRepo.findReactionsByPostId(postId);

        long total = stats != null ? stats.getReactions() : 0;

        // ❌ No per-type counts yet
        Map<ReactionType, Long> counts = Map.of();

        // =====================================
        // 🚀 REALTIME EVENT
        // =====================================
        try {
            publisher.publishEvent(
                    new PostReactionRealtimeEvent(
                            postId,
                            finalUserReaction,
                            total,
                            counts
                    )
            );
        } catch (Exception e) {
            log.warn("Reaction realtime failed postId={}", postId, e);
        }

        return PostReactionResponse.builder()
                .postId(postId)
                .userReaction(finalUserReaction)
                .total(total)
                .counts(counts)
                .build();
    }
}