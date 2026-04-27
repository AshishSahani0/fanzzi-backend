package com.fanzzi.backend.channel.stats;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * ============================================================
 * CHANNEL STATS (HOT WRITE COLLECTION)
 * ============================================================
 *
 * - All counters stored here
 * - Updated via Redis batch flush
 * - High write throughput safe
 *
 * Shard Key Recommendation:
 * - channelId (hashed)
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("channel_stats")

@CompoundIndexes({

        // Trending query
        @CompoundIndex(
                name = "trending_idx",
                def = "{'memberCount':-1,'lastPostAt':-1}"
        ),

        // Activity ranking
        @CompoundIndex(
                name = "activity_idx",
                def = "{'lastPostAt':-1}"
        )
})
public class ChannelStats {

    @Id
    private String channelId;

    @Builder.Default
    private long memberCount = 0;

    @Builder.Default
    private long subscriberCount = 0;

    @Builder.Default
    private long postCount = 0;

    @Builder.Default
    private long viewCount = 0;

    @Builder.Default
    private long reactionCount = 0;

    @Builder.Default
    private long shareCount = 0;

    @Indexed
    private Instant lastPostAt;

    private Instant updatedAt;
}