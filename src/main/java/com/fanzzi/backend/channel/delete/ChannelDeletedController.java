package com.fanzzi.backend.channel.delete;

import com.fanzzi.backend.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelDeletedController {

    private final ChannelDeletedQueryService queryService;

    @GetMapping("/deleted")
    public PagedResponse<DeletedChannelDTO> getDeletedChannels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return queryService.getDeletedChannels(userId, page, size);
    }
}