package com.fanzzi.backend.wallets.stars.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

@Document(collection = "user_wallets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWallet implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    // One wallet per user (DB will enforce uniqueness)
    private String userId;

    // Spendable stars (deducted atomically in service)
    @Builder.Default
    private long purchasedStars = 0L;

    // Creator earnings
    @Builder.Default
    private long earnedStars = 0L;

    //Lifetime stats (never decrease)
    @Builder.Default
    private long lifetimePurchased = 0L;

    @Builder.Default
    private long lifetimeEarned = 0L;

    // Audit
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    //  Computed value (not stored)
    public long getTotalStars() {
        return purchasedStars + earnedStars;
    }
}