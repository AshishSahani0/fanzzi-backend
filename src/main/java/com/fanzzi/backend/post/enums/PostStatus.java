package com.fanzzi.backend.post.enums;

public enum PostStatus {

    DRAFT,
    PROCESSING,
    PUBLISHED,
    ARCHIVED,
    DELETED;

    // =====================================
    // 👁 VISIBILITY
    // =====================================

    public boolean isVisible() {
        return this == PUBLISHED;
    }

    public boolean shouldAppearInFeed() {
        return this == PUBLISHED;
    }

    // =====================================
    // ✏️ EDIT / DELETE RULES
    // =====================================

    public boolean canEdit() {
        return this == DRAFT || this == PUBLISHED;
    }

    public boolean canDelete() {
        return this != DELETED;
    }

    public boolean isTerminal() {
        return this == DELETED;
    }

    // =====================================
    // 🔁 TRANSITIONS
    // =====================================

    public boolean canTransitionTo(PostStatus target) {

        if (this == DELETED) return false;

        return switch (this) {
            case DRAFT -> target == PROCESSING || target == PUBLISHED;
            case PROCESSING -> target == PUBLISHED || target == DELETED;
            case PUBLISHED -> target == ARCHIVED || target == DELETED;
            case ARCHIVED -> target == PUBLISHED || target == DELETED;
            default -> false;
        };
    }
}