package com.fanzzi.backend.channel.message.service;

import com.fanzzi.backend.channel.message.service.ChannelMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChannelMessageServiceImpl implements ChannelMessageService {

    @Override
    public void sendSystemMessage(
            String channelId,
            Map<String, Object> payload
    ) {
        // TODO:
        // Save message in DB
        // Publish websocket event
        // Push notification

        System.out.println(
                "SYSTEM MESSAGE to channel " + channelId + ": " + payload
        );
    }
}