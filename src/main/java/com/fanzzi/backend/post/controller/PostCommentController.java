package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.dto.CommentPageResponse;
import com.fanzzi.backend.post.dto.CreateCommentRequest;
import com.fanzzi.backend.post.model.PostComment;
import com.fanzzi.backend.post.service.comments.PostCommentService;
import com.fanzzi.backend.security.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostCommentController {

    private final PostCommentService service;

    // =====================================
    // 🔥 CREATE COMMENT
    // =====================================
    @PostMapping("/posts/{postId}/comments")
    public PostComment createComment(
            @PathVariable String postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Create comment postId={} userId={}", postId, userId);

        return service.createComment(postId, userId, request);
    }

    // =====================================
    // 📥 GET COMMENTS
    // =====================================
    @GetMapping("/posts/{postId}/comments")
    public CommentPageResponse getComments(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page
    ) {

        log.debug("Fetch comments postId={} page={}", postId, page);

        return service.getComments(postId, page);
    }

    // =====================================
    // 💬 GET REPLIES
    // =====================================
    @GetMapping("/comments/{commentId}/replies")
    public Page<PostComment> getReplies(
            @PathVariable String commentId,
            @RequestParam(defaultValue = "0") int page
    ) {

        log.debug("Fetch replies commentId={} page={}", commentId, page);

        return service.getReplies(commentId, page);
    }

    // =====================================
    // 🔁 REPLY
    // =====================================
    @PostMapping("/comments/{commentId}/reply")
    public PostComment reply(
            @PathVariable String commentId,
            @Valid @RequestBody CreateCommentRequest request
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Reply commentId={} userId={}", commentId, userId);

        return service.createReply(commentId, userId, request);
    }

    // =====================================
    // 📌 PIN COMMENT
    // =====================================
    @PostMapping("/comments/{commentId}/pin")
    public PostComment pinComment(
            @PathVariable String commentId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Pin comment commentId={} userId={}", commentId, userId);

        return service.pinComment(commentId, userId);
    }
}