package com.fanzzi.backend.post.dto;


import com.fanzzi.backend.post.model.PostComment;

import java.util.List;

public record CommentPageResponse(
        List<PostComment> comments,
        boolean hasMore
) {}

