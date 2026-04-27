package com.fanzzi.backend.channel.message.service;



import java.util.Map;

public interface ChannelMessageService {

    void sendSystemMessage(
            String channelId,
            Map<String, Object> payload
    );
}
