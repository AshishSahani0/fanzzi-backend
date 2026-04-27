package com.fanzzi.backend.channel.status.model;

import com.fanzzi.backend.channel.status.dto.StatusMedia;
import com.fanzzi.backend.channel.status.dto.StatusType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("channel_status")

@CompoundIndexes({

        // 🚀 Fast feed query
        @CompoundIndex(
                name = "channel_active_created_idx",
                def = "{'channelId':1,'deleted':1,'expiresAt':1,'createdAt':-1}"
        ),

        // 🚀 Owner lookup
        @CompoundIndex(
                name = "owner_created_idx",
                def = "{'ownerId':1,'createdAt':-1}"
        ),

        // 🚀 Expiry cleanup
        @CompoundIndex(
                name = "expiry_idx",
                def = "{'expiresAt':1}"
        )
})
public class ChannelStatus {

    @Id
    private String id;

    @Indexed
    private String channelId;

    @Indexed
    private String ownerId;

    private StatusType type;

    private String text;

    private String backgroundColor;

    private List<StatusMedia> media;

    @Indexed
    private Instant createdAt;

    @Indexed
    private Instant expiresAt;

    private boolean deleted;

    // 🔥 NEW — optimize UI
    private long viewCount;

    // 🔥 NEW — last viewed timestamp (owner analytics)
    private Instant lastViewedAt;

    // 🔥 NEW — quick preview (avoid heavy media load)
    private String previewMediaKey;

    // 🔥 NEW — for grouping (day buckets)
    private String bucket; // e.g. "2026-03-29"
}