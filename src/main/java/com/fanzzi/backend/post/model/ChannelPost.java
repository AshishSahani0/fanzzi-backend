package com.fanzzi.backend.post.model;

import com.fanzzi.backend.post.dto.Poll;
import com.fanzzi.backend.post.dto.PostEditHistory;
import com.fanzzi.backend.post.enums.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("channel_posts")

@CompoundIndexes({

        // ==========================================
        // PRIMARY FEED INDEX (MOST IMPORTANT)
        // ==========================================
        @CompoundIndex(
                name = "channel_seq_feed_idx",
                def = "{'channelId':1,'seq':-1,'deleted':1}"
        ),

        // ==========================================
        // BUCKET FEED (DEEP HISTORY)
        // ==========================================
        @CompoundIndex(
                name = "channel_bucket_feed_idx",
                def = "{'channelId':1,'bucketId':1,'seq':-1,'deleted':1}"
        ),

        // ==========================================
        // PINNED POSTS
        // ==========================================
        @CompoundIndex(
                name = "channel_pinned_idx",
                def = "{'channelId':1,'pinned':1,'pinnedAt':-1}"
        ),

        // ==========================================
        // CREATOR POSTS
        // ==========================================
        @CompoundIndex(
                name = "creator_posts_idx",
                def = "{'postedByUserId':1,'createdAt':-1}"
        ),

        // ==========================================
        // SCHEDULED POSTS
        // ==========================================
        @CompoundIndex(
                name = "scheduled_posts_idx",
                def = "{'channelId':1,'scheduledAt':1}"
        ),

        // ==========================================
        // ENGAGEMENT SORT (FUTURE)
        // ==========================================
        @CompoundIndex(
                name = "channel_engagement_idx",
                def = "{'channelId':1,'reactionCount':-1,'commentCount':-1}"
        )
})

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelPost {

    // ==========================================
    // PRIMARY KEY
    // ==========================================
    @Id
    private String id;

    // ==========================================
    // CHANNEL
    // ==========================================
    @Indexed
    private String channelId;

    private long seq;
    private int bucketId;

    // ==========================================
    // CREATOR
    // ==========================================
    private String postedByUserId;

    // ==========================================
    // CONTENT
    // ==========================================
    private String text;

    private List<PostAttachment> attachments;

    /**
     * ⚠️ MUST ALWAYS MATCH attachments.size()
     */
    private int attachmentsCount;

    private Float aspectRatio;

    // ==========================================
    // CORE TYPES
    // ==========================================
    private ContentType contentType;
    private MonetizationType monetizationType;

    // ==========================================
    // MONETIZATION
    // ==========================================
    private long price;
    private int previewSeconds;

    // ==========================================
    // REPLY / FORWARD
    // ==========================================
    private String replyToPostId;

    private String forwardedFromChannelId;
    private String forwardedFromPostId;
    private long forwardCount;

    // ==========================================
    // POLL
    // ==========================================
    private Poll poll;

    // ==========================================
    // ENGAGEMENT
    // ==========================================
    private long commentCount;
    private long reactionCount;   // NEW
    private long viewCount;       // NEW

    // ==========================================
    // DUPLICATE DETECTION
    // ==========================================
    private String contentHash;

    // ==========================================
    // PINNED
    // ==========================================
    private boolean pinned;
    private Instant pinnedAt;
    private String pinnedBy;

    // ==========================================
    // SILENT /  SCHEDULED
    // ==========================================
    private boolean silent;
    private Instant scheduledAt;

    // ==========================================
    // ✏EDIT HISTORY
    // ==========================================
    private boolean edited;
    private List<PostEditHistory> editHistory;

    // ==========================================
    // 🚦 STATUS
    // ==========================================
    private PostStatus status;

    // ==========================================
    // 🗑 SOFT DELETE
    // ==========================================
    @Indexed
    private boolean deleted;

    private Instant deletedAt;


    private boolean downloadable;


    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant createdAt;

    private Instant updatedAt;



    public boolean isActive() {
        return !deleted && status == PostStatus.PUBLISHED;
    }

    public boolean isScheduled() {
        return scheduledAt != null && scheduledAt.isAfter(Instant.now());
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    public void updateAttachmentCount() {
        this.attachmentsCount =
                (attachments != null) ? attachments.size() : 0;
    }
}