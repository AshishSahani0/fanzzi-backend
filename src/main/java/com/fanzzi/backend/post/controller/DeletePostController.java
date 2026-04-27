package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.service.delete.DeletePostService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/channels/{channelId}/posts")
public class DeletePostController {

    private final DeletePostService service;

    // =====================================
    // 🗑️ DELETE POST
    // =====================================
    @DeleteMapping("/{postId}")
    public boolean delete(
            @PathVariable String channelId,
            @PathVariable String postId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Delete post request channelId={} postId={} userId={}",
                channelId, postId, userId);

        service.deletePost(channelId, postId);

        log.debug("Delete post success postId={}", postId);

        return true; // simple success response
    }
}