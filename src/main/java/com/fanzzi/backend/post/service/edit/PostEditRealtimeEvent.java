package com.fanzzi.backend.post.service.edit;

import com.fanzzi.backend.post.dto.PostResponse;

public record PostEditRealtimeEvent(
        String channelId,
        PostResponse post
) {}
