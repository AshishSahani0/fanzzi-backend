package com.fanzzi.backend.post.service.reaction;

import com.fanzzi.backend.post.enums.ReactionType;

import java.util.Map;

public record PostReactionRealtimeEvent(
        String postId,
        ReactionType userReaction,
        long total,
        Map<ReactionType, Long> counts
) {}
