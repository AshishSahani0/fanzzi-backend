package com.fanzzi.backend.post.enums;

public enum AttachmentType {

    IMAGE,
    VIDEO,
    AUDIO,
    VOICE,
    GIF,
    FILE,
    STICKER;



    public boolean isMedia() {
        return this == IMAGE || this == VIDEO || this == AUDIO || this == GIF;
    }

    public boolean isVideo() {
        return this == VIDEO;
    }

    public boolean isAudio() {
        return this == AUDIO || this == VOICE;
    }

    public boolean isDownloadable() {
        return this == FILE;
    }

    // =====================================
    // PREVIEW SUPPORT
    // =====================================

    public boolean supportsPreview() {
        return this == VIDEO || this == AUDIO || this == GIF;
    }

    // =====================================
    // MONETIZATION RULE
    // =====================================

    public boolean canBePaid() {
        return this == VIDEO || this == IMAGE || this == FILE;
    }

    // =====================================
    // 🛡 MIME VALIDATION
    // =====================================

    public boolean isValidMime(String mime) {
        if (mime == null) return false;

        return switch (this) {
            case IMAGE, GIF -> mime.startsWith("image/");
            case VIDEO -> mime.startsWith("video/");
            case AUDIO, VOICE -> mime.startsWith("audio/");
            case FILE -> true;
            case STICKER -> mime.startsWith("image/");
        };
    }
}