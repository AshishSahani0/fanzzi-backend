package com.fanzzi.backend.channel.delete;


import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelRestoreController {

    private final ChannelRestoreService restoreService;

    // =====================================================
    // 🔄 RESTORE CHANNEL (JWT BASED)
    // =====================================================
    @PostMapping("/{channelId}/restore")
    public Document restoreChannel(@PathVariable String channelId) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        RestoreResult result = restoreService.restore(channelId, userId);

        // 🔥 FETCH UPDATED CHANNEL
        Document channel = restoreService.getChannel(channelId);

        return channel;
    }
}