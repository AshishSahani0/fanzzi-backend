package com.fanzzi.backend.channel.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChannelEvent {

    private String channelId;
    private String userId; // who triggered
    private ChannelEventType type;

    private Object payload; // flexible (ChannelResponse, member count, etc)
}