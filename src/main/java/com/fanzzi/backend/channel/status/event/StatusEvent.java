package com.fanzzi.backend.channel.status.event;

import com.fanzzi.backend.channel.status.model.ChannelStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusEvent {

    private String channelId;
    private String userId;

    private StatusEventType type;

    private ChannelStatus payload;
}