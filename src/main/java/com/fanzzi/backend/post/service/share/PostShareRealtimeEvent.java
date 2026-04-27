package com.fanzzi.backend.post.service.share;

public record PostShareRealtimeEvent(
        String postId,
        long totalShares
) {}
