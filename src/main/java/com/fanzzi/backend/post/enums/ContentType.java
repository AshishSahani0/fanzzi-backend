package com.fanzzi.backend.post.enums;

public enum ContentType {

    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    POLL;

    // =====================================
    // 🔥 TYPE GROUPS
    // =====================================

    public boolean isMedia() {
        return this != TEXT && this != POLL;
    }

    public boolean isVisual() {
        return this == IMAGE || this == VIDEO;
    }

    public boolean isAudio() {
        return this == AUDIO;
    }

    // =====================================
    // 💰 MONETIZATION RULE
    // =====================================

    public boolean canBePaid() {
        return this == IMAGE || this == VIDEO || this == FILE;
    }

    // =====================================
    // 🎬 PREVIEW SUPPORT
    // =====================================

    public boolean supportsPreview() {
        return this == VIDEO || this == AUDIO;
    }

    // =====================================
    // ⬇️ DOWNLOAD SUPPORT
    // =====================================

    public boolean isDownloadable() {
        return this == FILE;
    }

    // =====================================
    // 🧠 DETECTION (OPTIONAL)
    // =====================================

    public static ContentType detect(boolean hasPoll, boolean hasAttachments, ContentType attachmentType) {

        if (hasPoll) return POLL;

        if (!hasAttachments) return TEXT;

        return attachmentType;
    }
}