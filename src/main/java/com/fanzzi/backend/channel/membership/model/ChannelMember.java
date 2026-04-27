package com.fanzzi.backend.channel.membership.model;

import com.fanzzi.backend.channel.enums.ChannelRole;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * ============================================================
 * CHANNEL MEMBERSHIP
 * ============================================================
 *
 * - One document per user per channel
 * - Never embed members inside Channel
 * - Safe for 5M+ member channels
 *
 * Shard Key Recommendation:
 * - channelId (hashed)
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("channel_members")

@CompoundIndexes({

        // Unique membership
        @CompoundIndex(
                name = "channel_user_unique_idx",
                def = "{'channelId':1,'userId':1}",
                unique = true
        ),

        // Active member pagination
        @CompoundIndex(
                name = "active_members_idx",
                def = "{'channelId':1,'left':1,'joinedAt':-1}"
        ),

        // User joined channels lookup
        @CompoundIndex(
                name = "user_channels_idx",
                def = "{'userId':1,'left':1}"
        )
})
public class ChannelMember {

    @Id
    private String id;

    @Indexed
    private String channelId;

    @Indexed
    private String userId;

    @Builder.Default
    private ChannelRole role = ChannelRole.MEMBER;

    private Instant joinedAt;

    // Soft leave
    @Builder.Default
    private boolean left = false;

    private Instant leftAt;

    private Instant lastReadAt;

    private Instant mutedUntil;
}