package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.dto.EditPostRequest;
import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.service.edit.EditPostService;
import com.fanzzi.backend.security.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/channels/{channelId}/posts")
public class EditPostController {

    private final EditPostService service;

    // =====================================
    // ✏️ EDIT POST
    // =====================================
    @PutMapping("/{postId}")
    public PostResponse edit(
            @PathVariable String channelId,
            @PathVariable String postId,
            @Valid @RequestBody EditPostRequest request
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Edit post request channelId={} postId={} userId={}",
                channelId, postId, userId);

        PostResponse response =
                service.editPost(channelId, postId, request);

        log.debug("Edit post success postId={}", postId);

        return response;
    }
}