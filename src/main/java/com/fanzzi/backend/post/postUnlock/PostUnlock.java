package com.fanzzi.backend.post.postUnlock;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("post_unlocks")
@CompoundIndexes({

        // ✅ Unique unlock per user per post (ONLY ONCE)
        @CompoundIndex(
                name = "user_post_unique",
                def = "{'userId':1,'postId':1}",
                unique = true
        ),

        // ✅ Fast user history (latest first)
        @CompoundIndex(
                name = "user_lookup_idx",
                def = "{'userId':1,'unlockedAt':-1}"
        ),

        // ✅ Channel analytics
        @CompoundIndex(
                name = "channel_lookup_idx",
                def = "{'channelId':1}"
        )
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostUnlock {

    @Id
    private String id;

    private String userId;
    private String postId;
    private String channelId;

    private long pricePaid;

    @Builder.Default
    private boolean refunded = false;

    private Instant refundedAt;

    private Instant unlockedAt;

    // ✅ IMPORTANT for payment tracking
    private String transactionId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // =====================================
    // ✅ HELPER (FUTURE SAFE)
    // =====================================
    public boolean isActive() {
        return !refunded;
    }
}