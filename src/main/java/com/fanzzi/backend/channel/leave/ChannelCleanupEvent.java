package com.fanzzi.backend.channel.leave;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChannelCleanupEvent {

    private String channelId;
    private String userId;

}