package com.fanzzi.backend.post.model;

import com.fanzzi.backend.post.enums.ReactionType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("post_reactions")

@CompoundIndexes({

        // =====================================
        // 🔥 FAST POST LOOKUP
        // =====================================
        @CompoundIndex(
                name = "post_reaction_idx",
                def = "{'postId':1}"
        ),

        // =====================================
        // 🔥 ONE REACTION PER USER
        // =====================================
        @CompoundIndex(
                name = "user_reaction_idx",
                def = "{'postId':1,'userId':1}",
                unique = true
        )
})
public class PostReaction {

    @Id
    private String id;

    // =====================================
    // 🔗 REFERENCES
    // =====================================
    @Indexed
    private String postId;

    @Indexed
    private String userId;

    // =====================================
    // ❤️ REACTION
    // =====================================
    private ReactionType reaction;

    // =====================================
    // ⏱ TIME
    // =====================================
    private Instant createdAt;

    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant updatedAt;

    // =====================================
    // 🔥 HELPERS
    // =====================================

    public boolean isSameReaction(ReactionType other) {
        return this.reaction == other;
    }

    public void updateReaction(ReactionType newReaction, Instant now) {
        this.reaction = newReaction;
        this.updatedAt = now;
    }
}