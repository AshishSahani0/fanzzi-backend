package com.fanzzi.backend.channel.query;

import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.common.dto.PagedResponse;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelQueryController {

    private final ChannelQueryService service;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    // =====================================================
    // 👑 MY CHANNELS
    // =====================================================

    @GetMapping("/my")
    public PagedResponse<ChannelResponse> myChannels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.getMyChannels(
                SecurityUtil.getCurrentUserId(),
                Math.max(page, DEFAULT_PAGE),
                Math.min(size, MAX_SIZE)
        );
    }

    // =====================================================
    // 👥 JOINED CHANNELS
    // =====================================================

    @GetMapping("/joined")
    public PagedResponse<ChannelResponse> joinedChannels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.getJoinedChannels(
                SecurityUtil.getCurrentUserId(),
                Math.max(page, DEFAULT_PAGE),
                Math.min(size, MAX_SIZE)
        );
    }

    // =====================================================
    // 🌎 EXPLORE
    // =====================================================

    @GetMapping("/explore")
    public PagedResponse<ChannelResponse> explore(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.explore(
                SecurityUtil.getCurrentUserId(),
                Math.max(page, DEFAULT_PAGE),
                Math.min(size, MAX_SIZE)
        );
    }

    @GetMapping("/archived")
    public List<ChannelResponse> getArchived() {

        String userId = SecurityUtil.getCurrentUserId();

        return service.getArchivedChannels(userId);
    }
}