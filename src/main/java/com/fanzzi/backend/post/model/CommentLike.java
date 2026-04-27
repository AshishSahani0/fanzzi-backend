package com.fanzzi.backend.post.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("comment_likes")
@Data
@CompoundIndexes({

        // =====================================
        // COMMENT LOOKUP
        // =====================================
        @CompoundIndex(
                name = "comment_like_idx",
                def = "{'commentId':1}"
        ),

        // =====================================
        // UNIQUE LIKE PER USER
        // =====================================
        @CompoundIndex(
                name = "user_like_idx",
                def = "{'commentId':1,'userId':1}",
                unique = true
        )
})
public class CommentLike {

    @Id
    private String id;

    // =====================================
    // REFERENCES
    // =====================================
    @Indexed
    private String commentId;

    @Indexed
    private String userId;

    // =====================================
    // ⏱ TIME
    // =====================================
    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant createdAt;

    // =====================================
    // HELPERS
    // =====================================
    public boolean isValid() {
        return commentId != null && userId != null;
    }
}