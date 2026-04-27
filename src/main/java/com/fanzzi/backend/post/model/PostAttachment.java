package com.fanzzi.backend.post.model;

import com.fanzzi.backend.post.enums.AttachmentType;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostAttachment implements Serializable {

    private String key;
    private AttachmentType type;

    private String thumbnailKey;
    private String previewKey;

    private String fileName;

    @Builder.Default
    private long fileSize = 0;

    private Integer width;
    private Integer height;
    private Integer duration;

    private String mimeType;

    // 🔥 NEW
    private int order;

    // =====================================
    // 🔥 HELPERS
    // =====================================

    public boolean isImage() {
        return type == AttachmentType.IMAGE || type == AttachmentType.GIF;
    }

    public boolean isVideo() {
        return type == AttachmentType.VIDEO;
    }

    public boolean isAudio() {
        return type == AttachmentType.AUDIO || type == AttachmentType.VOICE;
    }

    public boolean hasPreview() {
        return previewKey != null && !previewKey.isBlank();
    }

    public boolean hasThumbnail() {
        return thumbnailKey != null && !thumbnailKey.isBlank();
    }

    public void validate() {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Attachment key required");
        }

        if (type == null) {
            throw new IllegalArgumentException("Attachment type required");
        }

        if (type == AttachmentType.VIDEO && !hasThumbnail()) {
            throw new IllegalArgumentException("Video must have thumbnail");
        }

        if (isAudio() && duration == null) {
            throw new IllegalArgumentException("Audio must have duration");
        }
    }

    public String buildUrl(String cdnBaseUrl) {
        return key != null ? cdnBaseUrl + "/" + key : null;
    }

    public String buildThumbnailUrl(String cdnBaseUrl) {
        return thumbnailKey != null ? cdnBaseUrl + "/" + thumbnailKey : null;
    }
}