package com.fanzzi.backend.post.enums;

public enum EventType {

    // =====================================
    // 📝 POST EVENTS
    // =====================================
    POST_CREATED,
    POST_UPDATED,
    POST_DELETED,
    POST_PINNED,

    // =====================================
    // 💬 COMMENT EVENTS
    // =====================================
    COMMENT_CREATED,
    COMMENT_DELETED,
    COMMENT_LIKED,
    COMMENT_PINNED,

    // =====================================
    // ❤️ REACTION EVENTS
    // =====================================
    REACTION_UPDATED,

    // =====================================
    // 🗳 POLL EVENTS
    // =====================================
    POLL_VOTED,

    // =====================================
    // 🔓 MONETIZATION EVENTS
    // =====================================
    CONTENT_UNLOCKED;

    // =====================================
    // 🔥 HELPERS
    // =====================================

    public boolean isPostEvent() {
        return this.name().startsWith("POST_");
    }

    public boolean isCommentEvent() {
        return this.name().startsWith("COMMENT_");
    }

    public boolean isRealtimeCritical() {
        return this == POST_CREATED
                || this == COMMENT_CREATED
                || this == REACTION_UPDATED
                || this == POLL_VOTED;
    }
}