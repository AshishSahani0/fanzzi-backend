package com.fanzzi.backend.common.channel.port;

public interface ChannelAccessService {
    boolean isMember(String channelId, String userId);
}
