package com.fanzzi.backend.channel.block.controller;

import com.fanzzi.backend.channel.block.dto.BlockedChannelDto;
import com.fanzzi.backend.channel.block.service.ChannelBlockService;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/blocked-channels")
@RequiredArgsConstructor
public class ChannelBlockUserController {

    private final ChannelBlockService blockService;

    // 📋 Get all blocked channels for current user
    @GetMapping
    public List<BlockedChannelDto> getBlockedChannels() {
        String userId = SecurityUtil.getCurrentUserId();
        return blockService.getBlockedChannels(userId);
    }
}