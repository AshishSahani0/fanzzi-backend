package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.service.download.PostDownloadService;
import com.fanzzi.backend.post.service.download.DownloadResponse;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostDownloadController {

    private final PostDownloadService downloadService;

    /**
     * 🔥 Smart download API
     */
    @GetMapping("/{postId}/download")
    public DownloadResponse download(@PathVariable String postId) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Download requested postId={} userId={}", postId, userId);

        DownloadResponse response = downloadService.getDownloadData(postId);

        log.debug("Download success postId={} count={}", postId, response.getCount());

        return response;
    }
}