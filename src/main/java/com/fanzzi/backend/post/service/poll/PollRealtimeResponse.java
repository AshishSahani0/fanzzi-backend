package com.fanzzi.backend.post.service.poll;

import com.fanzzi.backend.post.dto.Poll;

public record PollRealtimeResponse(
        String postId,
        Poll poll,
        long timestamp
) {}
