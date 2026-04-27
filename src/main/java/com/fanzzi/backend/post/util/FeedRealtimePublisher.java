package com.fanzzi.backend.post.util;

import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.enums.EventType;
import com.fanzzi.backend.post.model.NewPostEvent;
import com.fanzzi.backend.post.pin.dto.PostPinRealtimeEvent;
import com.fanzzi.backend.post.service.edit.PostEditRealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedRealtimePublisher {

    private final SimpMessagingTemplate messaging;

    private static final String DESTINATION_PREFIX = "/topic/channel/";

    // =====================================
    // NEW POST
    // =====================================
    @Async
    @EventListener
    public void onNewPost(NewPostEvent event) {

        if (event == null || event.getChannelId() == null) return;

        send(
                event.getChannelId(),
                RealtimeMessage.<String>builder()
                        .type(EventType.POST_CREATED)
                        .channelId(event.getChannelId())
                        .seq(event.getSeq())
                        .timestamp(now())
                        .data(event.getPostId())
                        .build()
        );
    }

    // =====================================
    // EDIT POST
    // =====================================
    @Async
    @EventListener
    public void onEdit(PostEditRealtimeEvent event) {

        if (event == null || event.channelId() == null) return;

        send(
                event.channelId(),
                RealtimeMessage.<PostResponse>builder()
                        .type(EventType.POST_UPDATED)
                        .channelId(event.channelId())
                        .seq(event.post().getSeq())
                        .timestamp(now())
                        .data(event.post())
                        .build()
        );
    }

    // =====================================
    // DELETE POST
    // =====================================
    @Async
    @EventListener
    public void onDelete(PostDeleteRealtimeEvent event) {

        if (event == null || event.channelId() == null) return;

        send(
                event.channelId(),
                RealtimeMessage.<String>builder()
                        .type(EventType.POST_DELETED)
                        .channelId(event.channelId())
                        .seq(event.seq())
                        .timestamp(now())
                        .data(event.postId())
                        .build()
        );
    }

    // =====================================
    // PIN POST
    // =====================================
    @Async
    @EventListener
    public void onPin(PostPinRealtimeEvent event) {

        if (event == null || event.channelId() == null) return;

        send(
                event.channelId(),
                RealtimeMessage.<PostPinRealtimeEvent>builder()
                        .type(EventType.POST_PINNED)
                        .channelId(event.channelId())
                        .timestamp(now())
                        .data(event)
                        .build()
        );
    }

    // =====================================
    // CENTRAL SEND METHOD
    // =====================================
    private void send(String channelId, RealtimeMessage<?> message) {

        try {
            messaging.convertAndSend(
                    DESTINATION_PREFIX + channelId,
                    message
            );

        } catch (Exception e) {
            log.warn(
                    "Realtime push failed channelId={} type={} seq={}",
                    channelId,
                    message.getType(),
                    message.getSeq(),
                    e
            );
        }
    }

    private long now() {
        return Instant.now().toEpochMilli();
    }
}