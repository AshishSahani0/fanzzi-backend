package com.fanzzi.backend.gifts.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("channel_gifts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelGift {

    @Id
    private String id;

    private String channelId;
    private String senderUserId;
    private String senderUsername;
    private String ownerUserId;

    private String giftId;
    private String giftName;
    private String giftEmoji;

    private long price;

    private Instant createdAt;
}