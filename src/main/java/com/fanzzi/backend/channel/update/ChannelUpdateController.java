package com.fanzzi.backend.channel.update;

import com.fanzzi.backend.channel.dto.request.UpdateChannelRequest;
import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.security.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/{channelId}")
@RequiredArgsConstructor
public class ChannelUpdateController {

    private final ChannelUpdateService service;

    /**
     * Update channel settings (owner only)
     */
    @PutMapping
    public ChannelResponse updateChannel(
            @PathVariable String channelId,
            @Valid @RequestBody UpdateChannelRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey
    ) {
        String userId = SecurityUtil.getCurrentUserId();
        return service.updateChannel(channelId, userId, req, idemKey);
    }
}