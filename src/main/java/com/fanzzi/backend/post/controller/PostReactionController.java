package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.enums.ReactionType;
import com.fanzzi.backend.post.service.reaction.PostReactionService;
import com.fanzzi.backend.post.service.reaction.PostReactionResponse;
import com.fanzzi.backend.post.service.share.PostShareService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostReactionController {

    private final PostReactionService reactionService;
    private final PostShareService shareService;

    // =====================================
    // 🔥 REACT (UPDATED)
    // =====================================
    @PostMapping("/{postId}/react")
    public PostReactionResponse react(
            @PathVariable String postId,
            @RequestParam ReactionType type
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        return reactionService.react(postId, userId, type);
    }

    // =====================================
    // 🔥 SHARE (OPTIONAL RETURN)
    // =====================================
    @PostMapping("/{postId}/share")
    public void share(@PathVariable String postId) {

        shareService.recordShare(postId);
    }
}