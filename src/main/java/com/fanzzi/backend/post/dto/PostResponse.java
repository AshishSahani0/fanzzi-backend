package com.fanzzi.backend.post.dto;

import com.fanzzi.backend.post.enums.ContentType;
import com.fanzzi.backend.post.enums.MonetizationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PostResponse {

    // ==========================================
    // BASIC
    // ==========================================
    private String id;
    private long seq;

    private String channelId;
    private String ownerId;

    // ==========================================
    // CONTENT
    // ==========================================
    private String text;

    @Builder.Default
    private List<AttachmentResponse> attachments = List.of();

    private ContentType contentType;
    private MonetizationType monetizationType;

    // ==========================================
    // 💰 MONETIZATION
    // ==========================================
    private long price;
    private int previewSeconds;

    private boolean unlocked;

    // ==========================================
    // STATE
    // ==========================================
    private boolean edited;

    private boolean pinned;
    private Instant pinnedAt;

    // ==========================================
    // TIME
    // ==========================================
    private Instant updatedAt;
    private Instant createdAt;

    // ==========================================
    // STATS
    // ==========================================
    private long views;
    private long reactions;
    private long comments;
    private long shares;

    // ==========================================
    // ACCESS
    // ==========================================
    private boolean downloadable;
    private boolean canDownload;

    // ==========================================
    // UI HELPERS
    // ==========================================
    private boolean hasMedia;
    private boolean hasPoll;
    private boolean isPaid;

    // ==========================================
    // POLL
    // ==========================================
    private Poll poll;

    // ==========================================
    // 🔥 HELPERS
    // ==========================================
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
}