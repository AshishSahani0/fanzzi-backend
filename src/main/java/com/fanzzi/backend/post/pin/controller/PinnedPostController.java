package com.fanzzi.backend.post.pin.controller;

import com.fanzzi.backend.post.pin.dto.PinnedBannerResponse;
import com.fanzzi.backend.post.pin.dto.PinnedListResponse;
import com.fanzzi.backend.post.pin.service.PinnedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/channels/{channelId}/pinned")
public class PinnedPostController {

    private final PinnedPostService pinnedPostService;

    // 🔥 For small top banner
    @GetMapping("/banner")
    public PinnedBannerResponse getPinnedBanner(
            @PathVariable String channelId
    ) {
        return pinnedPostService.getPinnedBanner(channelId);
    }

    // 🔥 For expand view
    @GetMapping
    public PinnedListResponse getAllPinnedPosts(
            @PathVariable String channelId
    ) {
        return pinnedPostService.getAllPinnedPosts(channelId);
    }
}