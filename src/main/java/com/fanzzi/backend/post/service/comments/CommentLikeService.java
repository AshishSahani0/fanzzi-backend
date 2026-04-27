package com.fanzzi.backend.post.service.comments;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.model.CommentLike;
import com.fanzzi.backend.post.model.PostComment;
import com.fanzzi.backend.post.repository.CommentLikeRepository;
import com.fanzzi.backend.post.repository.PostCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository likeRepo;
    private final PostCommentRepository commentRepo;
    private final RedisCommentEngagementService redisService;

    // =====================================
    // 🔥 TOGGLE LIKE (SAFE + CONSISTENT)
    // =====================================
    @Transactional
    public boolean toggleLike(String commentId, String userId) {

        if (commentId == null || userId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        // =====================================
        // 🔒 VALIDATE COMMENT
        // =====================================
        PostComment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Comment not found"));

        try {

            CommentLike existing =
                    likeRepo.findByCommentIdAndUserId(commentId, userId)
                            .orElse(null);

            boolean liked;

            // ================= REMOVE LIKE
            if (existing != null) {

                likeRepo.delete(existing);
                liked = false;

            }
            // ================= ADD LIKE
            else {

                CommentLike like = new CommentLike();
                like.setCommentId(commentId);
                like.setUserId(userId);
                like.setCreatedAt(Instant.now());

                likeRepo.save(like);
                liked = true;
            }

            // =====================================
            // 🔥 REDIS UPDATE (AFTER DB SUCCESS)
            // =====================================
            try {
                redisService.toggleLike(commentId, userId);
            } catch (Exception e) {
                log.warn("Redis like update failed commentId={} userId={}", commentId, userId, e);
            }

            return liked;

        } catch (DuplicateKeyException e) {
            // 🔥 race condition protection
            log.warn("Duplicate like prevented commentId={} userId={}", commentId, userId);
            return true;
        } catch (Exception e) {

            log.warn("Comment like toggle failed commentId={} userId={}", commentId, userId, e);
            throw e;
        }
    }
}