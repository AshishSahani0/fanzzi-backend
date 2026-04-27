package com.fanzzi.backend.post.service.comments;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.dto.CommentCreatedEvent;
import com.fanzzi.backend.post.dto.CommentPageResponse;
import com.fanzzi.backend.post.dto.CreateCommentRequest;
import com.fanzzi.backend.post.model.PostComment;
import com.fanzzi.backend.post.repository.PostCommentRepository;
import com.fanzzi.backend.post.repository.PostStatsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCommentService {

    private static final int PAGE_SIZE = 20;

    private final PostCommentRepository repository;
    private final PostStatsRepository statsRepo;
    private final RedisCommentEngagementService redisService;
    private final PostCommentCacheService cacheService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;
    private final CommentRankingService rankingService;

    // =========================================
    // CREATE COMMENT
    // =========================================
    public PostComment createComment(String postId, String userId, CreateCommentRequest request) {

        if (postId == null || userId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Comment text cannot be empty");
        }

        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setText(request.getText().trim());
        comment.setCreatedAt(Instant.now());

        // ================= REPLY =================
        if (request.getParentCommentId() != null) {

            PostComment parent = repository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Parent comment not found"));

            if (parent.getDepth() >= 2) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Max reply depth reached");
            }

            comment.setParentCommentId(parent.getId());
            comment.setDepth(parent.getDepth() + 1);

            redisService.incrementReplies(parent.getId());

        } else {
            comment.setDepth(0);
        }

        // ================= SAVE =================
        PostComment saved = repository.save(comment);

        // ================= RANKING =================
        saved.setRankingScore(rankingService.calculateScore(saved));
        repository.save(saved);

        // ================= CACHE FIX =================
        if (saved.getDepth() == 0) {
            cacheService.clear(postId); // 🔥 avoid duplicates
        }

        // ================= STATS =================
        try {
            statsRepo.incrementComments(postId, Instant.now());
        } catch (Exception e) {
            log.warn("Comment stat increment failed postId={}", postId, e);
        }

        // ================= REALTIME =================
        publisher.publishEvent(new CommentCreatedEvent(postId, saved));

        return saved;
    }

    // =========================================
    // LOAD COMMENTS
    // =========================================
    public CommentPageResponse getComments(String postId, int page) {

        if (postId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        if (page < 0) page = 0;

        // ================= CACHE =================
        if (page == 0) {

            List<String> cached = cacheService.getCachedComments(postId, PAGE_SIZE);

            if (cached != null && !cached.isEmpty()) {

                List<PostComment> comments = cached.stream()
                        .map(json -> {
                            try {
                                return objectMapper.readValue(json, PostComment.class);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(c -> c != null)
                        .toList();

                comments.forEach(this::mergeEngagement);

                return new CommentPageResponse(comments, true);
            }
        }

        // ================= DB =================
        Page<PostComment> result =
                repository.findComments(
                        postId,
                        0,
                        PageRequest.of(page, PAGE_SIZE)
                                .withSort(
                                        Sort.by(
                                                Sort.Order.desc("pinned"),
                                                Sort.Order.desc("rankingScore"),
                                                Sort.Order.desc("createdAt")
                                        )
                                )
                );

        List<PostComment> comments = result.getContent();

        // ================= MERGE REDIS =================
        comments.forEach(this::mergeEngagement);

        // ================= CACHE WARM =================
        if (page == 0 && !comments.isEmpty()) {

            cacheService.clear(postId);

            comments.forEach(c -> {
                try {
                    cacheService.cacheComment(
                            postId,
                            objectMapper.writeValueAsString(c)
                    );
                } catch (Exception ignored) {}
            });
        }

        return new CommentPageResponse(comments, result.hasNext());
    }

    // =========================================
    // LOAD REPLIES
    // =========================================
    public Page<PostComment> getReplies(String commentId, int page) {

        Page<PostComment> replies =
                repository.findByParentCommentIdOrderByCreatedAtAsc(
                        commentId,
                        PageRequest.of(page, PAGE_SIZE)
                );

        replies.getContent().forEach(this::mergeEngagement);

        return replies;
    }

    // =========================================
    // CREATE REPLY
    // =========================================
    public PostComment createReply(String parentCommentId, String userId, CreateCommentRequest request) {

        PostComment parent = repository.findById(parentCommentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        request.setParentCommentId(parentCommentId);

        return createComment(parent.getPostId(), userId, request);
    }

    // =========================================
    // PIN COMMENT
    // =========================================
    public PostComment pinComment(String commentId, String userId) {

        PostComment comment = repository.findById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 🔥 TODO: replace with channel/post owner check
        if (!userId.equals(comment.getUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        repository.unpinAllByPostId(comment.getPostId());

        comment.setPinned(true);

        return repository.save(comment);
    }

    // =========================================
    // 🔥 MERGE REDIS ENGAGEMENT
    // =========================================
    private void mergeEngagement(PostComment comment) {

        try {
            long likes = redisService.getLikeCount(comment.getId());
            long replies = redisService.getReplyCount(comment.getId());

            comment.setLikes(comment.getLikes() + likes);
            comment.setReplyCount(comment.getReplyCount() + replies);

        } catch (Exception ignored) {}
    }
}