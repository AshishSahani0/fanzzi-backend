package com.fanzzi.backend.post.dto;

import com.fanzzi.backend.post.enums.ContentType;
import com.fanzzi.backend.post.enums.MonetizationType;
import com.fanzzi.backend.post.model.PostAttachment;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostEditHistory {

    // ==========================================
    // CONTENT SNAPSHOT
    // ==========================================
    private String previousText;

    @Builder.Default
    private List<PostAttachment> previousAttachments = List.of();

    private ContentType previousContentType;
    private MonetizationType previousMonetizationType;

    // 🗳 POLL (optional)
    private Poll previousPoll;

    // ==========================================
    // MONETIZATION SNAPSHOT
    // ==========================================
    private long previousPrice;
    private int previousPreviewSeconds;

    // ==========================================
    // DIFF (WHAT CHANGED)
    // ==========================================
    @Builder.Default
    private List<String> changedFields = List.of();

    // ==========================================
    // AUDIT
    // ==========================================
    private String editedBy;
    private boolean editedByOwner;

    private int version;
    private String editReason; // "user_edit", "moderation", etc.

    private Instant editedAt;

    // ==========================================
    // 🔒 DEFENSIVE SETTER
    // ==========================================
    public void setPreviousAttachments(List<PostAttachment> attachments) {
        this.previousAttachments = attachments == null
                ? List.of()
                : List.copyOf(attachments);
    }
}