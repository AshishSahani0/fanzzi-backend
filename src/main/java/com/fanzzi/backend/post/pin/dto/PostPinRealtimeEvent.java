package com.fanzzi.backend.post.pin.dto;

public record PostPinRealtimeEvent(
        String channelId,
        String postId,
        boolean pinned
) {}
