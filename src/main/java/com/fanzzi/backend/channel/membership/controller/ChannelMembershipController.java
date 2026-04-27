package com.fanzzi.backend.channel.membership.controller;

import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/channels/membership")
@RequiredArgsConstructor
public class ChannelMembershipController {

    private final ChannelMemberRepository memberRepository;

    @GetMapping("/{channelId}")
    public Map<String, Boolean> isMember(
            @PathVariable String channelId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        boolean isMember =
                memberRepository.existsByChannelIdAndUserIdAndLeftFalse(
                        channelId,
                        userId
                );

        return Map.of("member", isMember);
    }
}