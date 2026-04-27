package com.fanzzi.backend.post.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("poll_votes")

@CompoundIndexes({

        // =====================================
        // 🔥 PREVENT DUPLICATE SAME OPTION VOTE
        // =====================================
        @CompoundIndex(
                name = "poll_vote_unique",
                def = "{'postId':1,'userId':1,'optionId':1}",
                unique = true
        ),

        // =====================================
        // 🔥 FAST POST LOOKUP
        // =====================================
        @CompoundIndex(
                name = "post_votes_idx",
                def = "{'postId':1}"
        ),

        // =====================================
        // 🔥 USER LOOKUP
        // =====================================
        @CompoundIndex(
                name = "user_votes_idx",
                def = "{'userId':1}"
        )
})
@Data
public class PollVote {

    @Id
    private String id;

    // =====================================
    // 🔗 REFERENCES
    // =====================================
    private String postId;

    private String optionId;

    private String userId;

    // =====================================
    // ⏱ TIME
    // =====================================
    private Instant createdAt;

    private Instant updatedAt;

    // =====================================
    // 🔥 HELPERS
    // =====================================
    public boolean isValid() {
        return postId != null && userId != null && optionId != null;
    }
}