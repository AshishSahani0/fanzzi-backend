package com.fanzzi.backend.post.service.reaction;

import com.fanzzi.backend.post.enums.ReactionType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PostReactionResponse {
    private String postId;
    private ReactionType userReaction;
    private long total;
    private Map<ReactionType, Long> counts;
}
