package com.fanzzi.backend.channel.state.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("user_channel_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(def = "{'userId':1,'channelId':1}", unique = true)
public class UserChannelState {

    @Id
    private String id;

    private String userId;

    private String channelId;

    // user archived this channel
    private boolean archived;

    // optional future features
    private boolean muted;
    private boolean pinned;
}