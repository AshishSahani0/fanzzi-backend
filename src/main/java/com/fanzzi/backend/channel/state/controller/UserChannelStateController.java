package com.fanzzi.backend.channel.state.controller;

import com.fanzzi.backend.channel.state.service.UserChannelStateService;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/channels")
@RequiredArgsConstructor
public class UserChannelStateController {

    private final UserChannelStateService service;

    @PostMapping("/{channelId}/archive")
    public void archive(@PathVariable String channelId) {

        String userId = SecurityUtil.getCurrentUserId();

        service.archiveChannel(userId, channelId);
    }

    @PostMapping("/{channelId}/unarchive")
    public void unarchive(@PathVariable String channelId) {

        String userId = SecurityUtil.getCurrentUserId();

        service.unarchiveChannel(userId, channelId);
    }

    @PostMapping(value = "/archive", consumes = "application/json")
    public void archiveBulk(@RequestBody List<String> channelIds) {

        String userId = SecurityUtil.getCurrentUserId();

        service.archiveChannels(userId, channelIds);
    }


}