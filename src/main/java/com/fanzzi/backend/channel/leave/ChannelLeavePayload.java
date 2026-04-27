package com.fanzzi.backend.channel.leave;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChannelLeavePayload {

    private String channelId;
    private String userId;
    private long memberCount;
}