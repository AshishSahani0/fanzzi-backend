package com.fanzzi.backend.post.service.comments;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.model.PostComment;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.repository.PostCommentRepository;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentPinService {

    private final PostCommentRepository repository;
    private final ApplicationEventPublisher publisher;
    private final ChannelPostRepository postRepository;

    // =====================================
    // 🔥 TOGGLE PIN (PRODUCTION SAFE)
    // =====================================
    @Transactional
    public PostComment togglePin(String commentId) {

        if (commentId == null || commentId.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid commentId");
        }

        // =====================================
        // LOAD COMMENT
        // =====================================
        PostComment comment = repository.findById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));

        String userId = SecurityUtil.getCurrentUserId();

        // =====================================
        // 🔒 PERMISSION CHECK (FIXED)
        // =====================================
        ChannelPost post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));

        if (!userId.equals(post.getPostedByUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not allowed to pin comment");
        }

        boolean newState = !comment.isPinned();

        // =====================================
        // 🔥 ENSURE SINGLE PIN PER POST
        // =====================================
        if (newState) {

            // avoid unnecessary DB call
            if (!repository.existsPinnedByPostId(comment.getPostId())) {
                // nothing pinned → skip unpin
            } else {
                repository.unpinAllByPostId(comment.getPostId());
            }
        }

        // =====================================
        // UPDATE
        // =====================================
        comment.setPinned(newState);

        PostComment saved = repository.save(comment);

        log.debug("Comment pin toggled postId={} commentId={} pinned={}",
                saved.getPostId(), saved.getId(), newState);

        // =====================================
        // 🚀 REALTIME EVENT
        // =====================================
        try {
            publisher.publishEvent(
                    new CommentPinEvent(
                            saved.getPostId(),
                            saved.getId(),
                            newState
                    )
            );
        } catch (Exception e) {
            log.warn("Comment pin realtime failed postId={} commentId={}",
                    saved.getPostId(), saved.getId(), e);
        }

        return saved;
    }
}