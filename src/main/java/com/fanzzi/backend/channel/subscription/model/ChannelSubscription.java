package com.fanzzi.backend.channel.subscription.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "channel_subscriptions")
@CompoundIndexes({

        // 🔐 One active subscription per user per channel
        @CompoundIndex(
                name = "user_channel_unique",
                def = "{'userId':1,'channelId':1}",
                unique = true
        ),

        // 🔥 Fast channel subscriber queries
        @CompoundIndex(
                name = "channel_active_idx",
                def = "{'channelId':1,'active':1,'expiresAt':-1}"
        ),

        // 🔥 Expiry cleanup job index
        @CompoundIndex(
                name = "expiry_idx",
                def = "{'expiresAt':1}"
        )
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSubscription {

    @Id
    private String id;

    private String userId;
    private String channelId;
    private String channelOwnerId;

    private Instant subscribedAt;
    private Instant expiresAt;

    private boolean active;

    // 💰 store price at subscription time
    private long pricePaid;

    // 💳 for webhook / order tracing
    private String paymentReference;

    // 🔁 renewal tracking
    @Builder.Default
    private int renewalCount = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}