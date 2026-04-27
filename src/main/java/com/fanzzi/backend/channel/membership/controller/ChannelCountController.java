package com.fanzzi.backend.channel.membership.controller;

import com.fanzzi.backend.channel.membership.service.ChannelCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelCountController {

    private final ChannelCountService countService;

    @GetMapping("/{channelId}/members/count")
    public Map<String, Long> getMemberCount(
            @PathVariable String channelId
    ) {

        long count = countService.getMemberCount(channelId);

        return Map.of("count", count);
    }
}