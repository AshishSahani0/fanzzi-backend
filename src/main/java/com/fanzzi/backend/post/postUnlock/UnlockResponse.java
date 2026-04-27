package com.fanzzi.backend.post.postUnlock;

import java.time.Instant;

public record UnlockResponse(

        String postId,

        // 🔓 ACCESS STATE
        boolean unlocked,

        // 💰 PRICE INFO
        long price,
        boolean paid,          // was this paid or free
        boolean ownerAccess,   // owner bypass

        // ⏱️ TIMING
        Instant unlockedAt,

        // 💳 TRANSACTION (IMPORTANT)
        String transactionId

) {}