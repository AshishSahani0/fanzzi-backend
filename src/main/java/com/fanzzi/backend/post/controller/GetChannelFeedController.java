package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.service.feed.GetChannelFeedService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/channels/{channelId}/feed")
@RequiredArgsConstructor
public class GetChannelFeedController {

    private final GetChannelFeedService service;

    @GetMapping
    public List<PostResponse> getFeed(
            @PathVariable String channelId,
            @RequestParam(required = false) Long beforeSeq
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Feed request channelId={} userId={} beforeSeq={}",
                channelId, userId, beforeSeq);

        List<PostResponse> feed = service.execute(channelId, beforeSeq);

        log.debug("Feed response size={} channelId={}",
                feed.size(), channelId);

        return feed;
    }
}