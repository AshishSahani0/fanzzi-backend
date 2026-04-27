package com.fanzzi.backend.post.util;

public record PostDeleteRealtimeEvent(
        String channelId,
        String postId,
        long seq
) {}
