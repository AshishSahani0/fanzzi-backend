package com.fanzzi.backend.channel.model;

import com.fanzzi.backend.channel.enums.ChannelType;
import com.fanzzi.backend.channel.enums.ChannelVisibility;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * ============================================================
 * CHANNEL DOCUMENT
 * ============================================================
 *
 * Represents a creator-owned community.
 *
 * Design Goals:
 * - Optimized for read-heavy workloads
 * - Safe for high concurrency counter updates
 * - Index-efficient for discover and dashboard queries
 * - Shard-ready for future MongoDB horizontal scaling
 * - Minimal document size to avoid write amplification
 *
 * Notes:
 * - All counter updates must use atomic $inc operations.
 * - Avoid full document rewrites for performance.
 * - At very high scale (5M+ active users), move counters
 *   to a separate collection or Redis-backed aggregation.
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("channels")

@CompoundIndexes({

        /**
         * Owner dashboard query
         * Used for fetching channels created by a specific user.
         * Optimized for pagination sorted by creation date.
         */
        @CompoundIndex(
                name = "owner_created_idx",
                def = "{'ownerId':1,'createdAt':-1}"
        ),

        /**
         * Discover page query
         * Filters only public, discoverable, non-deleted channels.
         * Sorted by popularity.
         */
        @CompoundIndex(
                name = "discover_idx",
                def = "{'visibility':1,'discoverable':1,'deleted':1,'memberCount':-1}"
        ),

        /**
         * Trending channels query
         * Prioritizes active channels by recent activity and size.
         */
        @CompoundIndex(
                name = "trending_idx",
                def = "{'visibility':1,'lastPostAt':-1,'memberCount':-1}"
        ),

        /**
         * Moderation dashboard
         */
        @CompoundIndex(
                name = "moderation_idx",
                def = "{'moderationStatus':1,'createdAt':-1}"
        ),

        /**
         * Category filtering for explore
         */
        @CompoundIndex(
                name = "category_visibility_idx",
                def = "{'category':1,'visibility':1,'memberCount':-1}"
        ),

        /**
         * Active channel filtering
         */
        @CompoundIndex(
                name = "active_idx",
                def = "{'deleted':1,'visibility':1}"
        )
})
public class Channel {

    /**
     * Primary identifier.
     * MongoDB ObjectId by default.
     * Suitable for hashed sharding in future.
     */
    @Id
    private String id;

    /**
     * Channel owner.
     * Frequently queried field, therefore indexed.
     */
    @Indexed
    private String ownerId;

    private long lastPostSeq;
    private int currentBucket;

    /**
     * Display name.
     */
    private String name;

    /**
     * Lowercase name used for case-insensitive search.
     * Indexed separately for fast lookup.
     */
    @Indexed
    private String nameLower;

    /**
     * Channel description text.
     */
    private String description;

    /**
     * CDN storage key for profile image.
     */
    private String profileImageKey;

    /**
     * Optional banner image.
     */
    private String bannerImageKey;

    /**
     * Public or private visibility.
     */
    @Indexed
    private ChannelVisibility visibility;

    /**
     * Free or paid channel type.
     */
    @Indexed
    private ChannelType type;

    /**
     * Subscription pricing configuration.
     */
    private Long monthlyPrice;
    private Long yearlyPrice;
    private Long entryFee;
    private Integer freeTrialDays;
    private String tierName;

    /**
     * Unique slug for public URL access.
     * Sparse index allows null values for private channels.
     */
    @Indexed(unique = true, sparse = true)
    private String slug;

    /**
     * Unique invite token for private channels.
     */
    @Indexed(unique = true, sparse = true)
    private String inviteToken;

    /**
     * Classification fields for discover filtering.
     */
    @Indexed
    private String category;

    @Indexed
    private String language;

    @Indexed
    private String region;

    /**
     * Discoverability flag.
     */
    @Builder.Default
    private boolean discoverable = true;

    /**
     * Content safety flag.
     */
    @Indexed
    @Builder.Default
    private boolean nsfw = false;

    /**
     * Whether join approval is required.
     */
    @Builder.Default
    private boolean joinApprovalRequired = false;

    /**
     * Whether comments are allowed.
     */
    @Builder.Default
    private boolean allowComments = true;

    /**
     * Denormalized counters.
     *
     * Important:
     * - Must be updated using atomic $inc.
     * - Never rewrite full document for counter updates.
     * - Consider separate stats collection if write load becomes extreme.
     */
    @Builder.Default
    private long memberCount = 0;

    @Builder.Default
    private long subscriberCount = 0;

    @Builder.Default
    private long postCount = 0;


    @Builder.Default
    private long viewCount = 0;

    @Builder.Default
    private long shareCount = 0;

    @Builder.Default
    private long reactionCount = 0;

    /**
     * Last post timestamp.
     * Used for activity-based ranking.
     */
    @Indexed
    private Instant lastPostAt;

    /**
     * Moderation status for admin workflows.
     */
    @Indexed
    @Builder.Default
    private ChannelModerationStatus moderationStatus =
            ChannelModerationStatus.NORMAL;

    /**
     * Soft delete flag.
     * Deleted channels are hidden from queries but retained for recovery.
     */
    @Indexed
    @Builder.Default
    private boolean deleted = false;

    private Instant deletedAt;

    /**
     * Creation timestamp.
     * Indexed for sorting and pagination.
     */
    @Indexed
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Updated timestamp.
     */
    private Instant updatedAt;
}