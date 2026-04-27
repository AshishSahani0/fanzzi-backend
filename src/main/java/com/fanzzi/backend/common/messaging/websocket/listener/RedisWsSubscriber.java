package com.fanzzi.backend.common.messaging.websocket.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RedisWsSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String CHANNEL_PREFIX = "channel.";
    private static final String USER_PREFIX = "user.";

    @Override
    public void onMessage(Message message, byte[] pattern) {

        try {
            // ✅ SAFE decoding
            String topic = new String(message.getChannel(), StandardCharsets.UTF_8);

            // ✅ IMPORTANT: use raw body (already serialized properly)
            byte[] body = message.getBody();

            // ================= CHANNEL =================
            if (topic.startsWith(CHANNEL_PREFIX)) {

                String channelId = topic.substring(CHANNEL_PREFIX.length());

                messagingTemplate.convertAndSend(
                        "/topic/channel/" + channelId,
                        body   // 🔥 send raw → avoids re-serialization cost
                );
            }

            // ================= USER =================
            else if (topic.startsWith(USER_PREFIX)) {

                String userId = topic.substring(USER_PREFIX.length());

                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/messages",
                        body
                );
            }

        } catch (Exception e) {
            // 🔥 NEVER crash listener (important at scale)
            // optionally log: log.error("Redis WS error", e);
        }
    }
}