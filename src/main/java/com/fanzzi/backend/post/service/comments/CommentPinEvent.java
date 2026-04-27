package com.fanzzi.backend.post.service.comments;

public record CommentPinEvent(
        String postId,
        String commentId,
        boolean pinned
) {}