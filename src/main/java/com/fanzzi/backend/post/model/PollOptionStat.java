package com.fanzzi.backend.post.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("poll_option_stats")

@CompoundIndexes({

        // =====================================
        // 🔥 UNIQUE OPTION PER POST
        // =====================================
        @CompoundIndex(
                name = "post_option_unique",
                def = "{'postId':1,'optionId':1}",
                unique = true
        ),

        // =====================================
        // 🔥 FAST POST LOOKUP
        // =====================================
        @CompoundIndex(
                name = "post_lookup_idx",
                def = "{'postId':1}"
        )
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PollOptionStat {

    @Id
    private String id;

    // =====================================
    // 🔗 REFERENCES
    // =====================================
    @Indexed
    private String postId;

    private String optionId;

    // =====================================
    // 📊 VOTES
    // =====================================
    @Builder.Default
    private long votes = 0;

    // =====================================
    // ⏱ TIME
    // =====================================
    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant updatedAt;

    // =====================================
    // 🔥 HELPERS
    // =====================================
    public void increment() {
        this.votes++;
        this.updatedAt = Instant.now();
    }
}