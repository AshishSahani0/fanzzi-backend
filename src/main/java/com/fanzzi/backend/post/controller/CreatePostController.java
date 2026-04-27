package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.dto.CreatePostRequest;
import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.service.create.CreatePostService;
import com.fanzzi.backend.security.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/channels/{channelId}/posts")
@RequiredArgsConstructor
public class CreatePostController {

    private final CreatePostService service;

    // =====================================
    // 📝 CREATE POST
    // =====================================
    @PostMapping
    public PostResponse createPost(
            @PathVariable String channelId,
            @Valid @RequestBody CreatePostRequest request
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Create post request channelId={} userId={}", channelId, userId);

        PostResponse response = service.createPost(channelId, request);

        log.debug("Create post success postId={} channelId={}",
                response.getId(), channelId);

        return response;
    }
}