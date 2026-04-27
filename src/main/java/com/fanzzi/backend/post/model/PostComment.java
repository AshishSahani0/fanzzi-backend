package com.fanzzi.backend.post.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("post_comments")
@Data
@CompoundIndexes({

        // =====================================
        // 🔥 POST COMMENTS (PRIMARY FEED)
        // =====================================
        @CompoundIndex(
                name = "post_comment_depth_idx",
                def = "{'postId':1,'depth':1,'createdAt':-1}"
        ),

        // =====================================
        // 🔁 REPLIES
        // =====================================
        @CompoundIndex(
                name = "reply_idx",
                def = "{'parentCommentId':1,'createdAt':1}"
        ),

        // =====================================
        // ⭐ RANKING
        // =====================================
        @CompoundIndex(
                name = "comment_ranking_idx",
                def = "{'postId':1,'likes':-1,'replyCount':-1,'createdAt':-1}"
        ),

        @CompoundIndex(
                name = "comment_rank_idx",
                def = "{'postId':1,'depth':1,'rankingScore':-1}"
        )
})
public class PostComment {

    @Id
    private String id;

    // =====================================
    // 🔗 REFERENCES
    // =====================================
    @Indexed
    private String postId;

    @Indexed
    private String userId;

    @Indexed
    private String parentCommentId;

    // =====================================
    // 📝 CONTENT
    // =====================================
    private String text;

    // =====================================
    // 🧵 THREADING
    // =====================================
    private int depth;

    // =====================================
    // 📊 COUNTERS
    // =====================================
    private long replyCount;
    private long likes;
    private double rankingScore;

    // =====================================
    // 📌 PINNING
    // =====================================
    private boolean pinned;

    // =====================================
    // 🗑 SOFT DELETE
    // =====================================
    private boolean deleted;
    private Instant deletedAt;

    // =====================================
    // ✏️ EDIT
    // =====================================
    private boolean edited;

    // =====================================
    // ⏱ TIME
    // =====================================
    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant createdAt;

    private Instant updatedAt;

    // =====================================
    // 🔥 HELPERS
    // =====================================

    public boolean isRoot() {
        return parentCommentId == null;
    }

    public boolean isReply() {
        return parentCommentId != null;
    }

    public boolean isActive() {
        return !deleted;
    }

    public void incrementLikes() {
        this.likes++;
    }

    public void decrementLikes() {
        if (this.likes > 0) this.likes--;
    }

    public void incrementReplyCount() {
        this.replyCount++;
    }

    public void decrementReplyCount() {
        if (this.replyCount > 0) this.replyCount--;
    }
}