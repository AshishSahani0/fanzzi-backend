package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.dto.Poll;
import com.fanzzi.backend.post.service.poll.PollService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PollController {

    private final PollService pollService;

    // =====================================
    // 🗳️ VOTE
    // =====================================
    @PostMapping("/{postId}/poll/vote")
    public Poll vote(
            @PathVariable String postId,
            @RequestParam String optionId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Poll vote postId={} optionId={} userId={}", postId, optionId, userId);

        return pollService.vote(postId, optionId, userId);
    }


}