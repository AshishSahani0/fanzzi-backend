package com.fanzzi.backend.channel.query;

import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.common.dto.PagedResponse;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/search")
@RequiredArgsConstructor
public class ChannelSearchController {

    private final ChannelSearchService service;

    private static final int MAX_SIZE = 50;

    @GetMapping("/public")
    public PagedResponse<ChannelResponse> searchPublic(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.searchPublic(
                SecurityUtil.getCurrentUserId(),
                q,
                Math.max(page, 0),
                Math.min(size, MAX_SIZE)
        );
    }
}