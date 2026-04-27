package com.fanzzi.backend.channel.leave;

import com.fanzzi.backend.common.dto.ApiMessageResponse;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/{channelId}")
@RequiredArgsConstructor
public class ChannelLeaveController {

    private final ChannelLeaveService leaveService;

    // =====================================================
    // 🚪 LEAVE CHANNEL
    // =====================================================

    @PostMapping("/leave")
    public ApiMessageResponse leave(@PathVariable String channelId) {

        String userId = SecurityUtil.getCurrentUserId();

        leaveService.leave(channelId, userId);

        return ApiMessageResponse.success("Left channel successfully");
    }
}