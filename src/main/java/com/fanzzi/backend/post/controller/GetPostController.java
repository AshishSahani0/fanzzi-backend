package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.service.post.GetPostService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class GetPostController {

    private final GetPostService service;

    @GetMapping("/{postId}")
    public PostResponse getPost(@PathVariable String postId) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Get post request postId={} userId={}", postId, userId);

        PostResponse response = service.getPost(postId, userId);

        log.debug("Get post success postId={}", postId);

        return response;
    }
}