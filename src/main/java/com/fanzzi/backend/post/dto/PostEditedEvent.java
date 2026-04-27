package com.fanzzi.backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostEditedEvent {

    private String channelId;
    private String postId;
    private long seq;
}