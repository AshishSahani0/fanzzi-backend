package com.fanzzi.backend.post.postUnlock;

public record UnlockRealtimeEvent(
        String postId,
        String userId
) {}