package com.fanzzi.backend.wallets.stars.transaction;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "star_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarTransaction {

    @Id
    private String id;

    private String userId;        // owner of this transaction
    private String channelId;     // related channel (if applicable)

    private long amount;          // + credit, - debit

    private StarTxnType type;

    private String referenceId;   // postId / subscriptionId / orderId

    private String description;

    private Instant createdAt;
}