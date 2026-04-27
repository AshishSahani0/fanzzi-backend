package com.fanzzi.backend.post.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Comment text is required")
    @Size(max = 500, message = "Comment too long")
    @Pattern(regexp = ".*\\S.*", message = "Comment cannot be empty")
    private String text;

    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{8,}$",
            message = "Invalid parent comment ID"
    )
    private String parentCommentId;


    public String getText() {
        return text != null ? text.trim() : null;
    }

    public boolean isReply() {
        return parentCommentId != null && !parentCommentId.isBlank();
    }
}