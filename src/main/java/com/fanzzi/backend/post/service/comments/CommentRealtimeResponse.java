package com.fanzzi.backend.post.service.comments;

import com.fanzzi.backend.post.model.PostComment;

public record CommentRealtimeResponse(
        String postId,
        PostComment comment,
        long timestamp
) {}