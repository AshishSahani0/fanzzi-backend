package com.fanzzi.backend.channel.block.model;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("channel_blocks")
@CompoundIndex(
        name = "user_channel_unique",
        def = "{'channelId':1,'userId':1}",
        unique = true
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelBlock {

    @Id
    private String id;

    private String channelId;

    @Indexed
    private String userId;

    private Instant blockedAt;
}
