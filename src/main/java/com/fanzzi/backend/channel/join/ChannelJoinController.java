package com.fanzzi.backend.channel.join;

import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.common.dto.ApiMessageResponse;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/join")
@RequiredArgsConstructor
public class ChannelJoinController {

    private final ChannelJoinService joinService;

    // =====================================================
    // ✅ Join Public Channel
    // =====================================================

    @PostMapping("/slug/{slug}")
    public ChannelResponse joinPublic(@PathVariable String slug) {

        String userId = SecurityUtil.getCurrentUserId();

        return joinService.joinBySlug(slug, userId);
    }

    // =====================================================
    // ✅ Join Private Channel
    // =====================================================

    @PostMapping("/invite/{token}")
    public ChannelResponse joinPrivate(@PathVariable String token) {

        String userId = SecurityUtil.getCurrentUserId();

        return joinService.joinByInviteToken(token, userId);


    }

    @PostMapping("/{channelId}")
    public ChannelResponse joinById(@PathVariable String channelId) {
        String userId = SecurityUtil.getCurrentUserId();
        return joinService.joinByChannelId(channelId, userId);
    }
}