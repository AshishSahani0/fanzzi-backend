package com.fanzzi.backend.post.enums;

public enum MonetizationType {

    FREE,
    PAID;

    // =====================================
    // 🔥 BASIC CHECKS
    // =====================================

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isFree() {
        return this == FREE;
    }

    // =====================================
    // 💰 RULES
    // =====================================

    public boolean requiresPrice() {
        return this == PAID;
    }

    // =====================================
    // 🔓 ACCESS CONTROL
    // =====================================

    public boolean canAccess(boolean unlocked, boolean isOwner) {

        if (this == FREE) return true;

        return unlocked || isOwner;
    }
}