package com.fanzzi.backend.post.model;

import com.fanzzi.backend.post.enums.ReactionType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document("post_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostStats {

    @Id
    private String postId;

    // =====================================
    // 📊 COUNTERS
    // =====================================
    @Builder.Default
    private long views = 0;

    @Builder.Default
    private long shares = 0;

    @Builder.Default
    private long comments = 0;

    @Builder.Default
    private long reactions = 0;

    // =====================================
    // ❤️ REACTION BREAKDOWN
    // =====================================
    @Builder.Default
    private Map<ReactionType, Long> reactionMap = new HashMap<>();

    // =====================================
    // ⏱ TIME
    // =====================================
    @Indexed
    private Instant updatedAt;

    // =====================================
    // 🔥 HELPERS
    // =====================================

    public void incrementViews() {
        this.views++;
    }

    public void incrementShares() {
        this.shares++;
    }

    public void incrementComments() {
        this.comments++;
    }

    public void decrementComments() {
        if (comments > 0) comments--;
    }

    public void incrementReaction(ReactionType type) {
        reactionMap.merge(type, 1L, Long::sum);
        reactions++;
    }

    public void decrementReaction(ReactionType type) {
        reactionMap.computeIfPresent(type, (k, v) -> v > 1 ? v - 1 : null);
        if (reactions > 0) reactions--;
    }
}