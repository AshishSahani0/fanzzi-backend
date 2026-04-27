package com.fanzzi.backend.channel.delete;

import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/{channelId}")
@RequiredArgsConstructor
public class ChannelDeleteController {

    private final ChannelDeleteService service;

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String channelId) {
        String userId = SecurityUtil.getCurrentUserId();
        service.delete(channelId, userId);
    }
}