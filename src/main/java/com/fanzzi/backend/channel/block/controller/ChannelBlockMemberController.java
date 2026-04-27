package com.fanzzi.backend.channel.block.controller;

import com.fanzzi.backend.channel.block.service.ChannelBlockService;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/{channelId}/block")
@RequiredArgsConstructor
public class ChannelBlockMemberController {

    private final ChannelBlockService blockService;

    @PostMapping
    public void block(@PathVariable String channelId) {
        String userId = SecurityUtil.getCurrentUserId();
        blockService.blockChannel(channelId, userId);
    }

    @DeleteMapping
    public void unblock(@PathVariable String channelId) {
        String userId = SecurityUtil.getCurrentUserId();
        blockService.unblockChannel(channelId, userId);
    }

    @GetMapping
    public boolean isBlocked(@PathVariable String channelId) {
        String userId = SecurityUtil.getCurrentUserId();
        return blockService.isBlocked(channelId, userId);
    }
}