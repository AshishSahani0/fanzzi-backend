package com.fanzzi.backend.post.enums;

import lombok.Getter;

@Getter
public enum ReactionType {

    LIKE("👍", 1),
    HEART("❤️", 2),
    FIRE("🔥", 3),
    CLAP("👏", 4),
    LAUGH("😂", 5);

    private final String emoji;
    private final int order;

    ReactionType(String emoji, int order) {
        this.emoji = emoji;
        this.order = order;
    }

    // =====================================
    // 🎨 UI SUPPORT
    // =====================================

    // =====================================
    // 🔥 HELPERS
    // =====================================

    public boolean isPositive() {
        return this != LAUGH;
    }

    // =====================================
    // 🛡 SAFE PARSE
    // =====================================

    public static ReactionType from(String value) {
        if (value == null) return LIKE;

        try {
            return ReactionType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return LIKE;
        }
    }
}