package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.service.comments.CommentLikeService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentLikeController {

    private final CommentLikeService service;

    // =====================================
    // ❤️ TOGGLE LIKE
    // =====================================
    @PostMapping("/{commentId}/like")
    public boolean like(@PathVariable String commentId) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Comment like toggle commentId={} userId={}", commentId, userId);

        boolean liked = service.toggleLike(commentId, userId);

        log.debug("Comment like result commentId={} liked={}", commentId, liked);

        return liked;
    }
}