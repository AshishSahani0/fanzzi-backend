package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.service.feed.FeedRecoveryService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/channels/{channelId}/feed")
@RequiredArgsConstructor
public class FeedRecoveryController {

    private final FeedRecoveryService recoveryService;

    // =====================================
    // 🔥 RECOVER MISSED POSTS
    // =====================================
    @GetMapping("/recover")
    public List<PostResponse> recover(
            @PathVariable String channelId,
            @RequestParam long lastSeenSeq
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Feed recovery request channelId={} userId={} lastSeenSeq={}",
                channelId, userId, lastSeenSeq);

        List<PostResponse> posts =
                recoveryService.recover(channelId, lastSeenSeq);

        log.debug("Feed recovery response size={} channelId={}",
                posts.size(), channelId);

        return posts;
    }
}