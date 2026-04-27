package com.fanzzi.backend.post.dto;


import com.fanzzi.backend.post.model.PostComment;

public record CommentCreatedEvent(
        String postId,
        PostComment comment
) {}

