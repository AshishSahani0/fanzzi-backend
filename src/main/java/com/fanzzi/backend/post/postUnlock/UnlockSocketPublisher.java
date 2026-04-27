package com.fanzzi.backend.post.postUnlock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnlockSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String DEST = "/topic/unlock/";

    @Async
    @EventListener
    public void onUnlock(UnlockRealtimeEvent event) {

        try {
            messagingTemplate.convertAndSend(
                    DEST + event.userId(),
                    event.postId()
            );
        } catch (Exception e) {
            log.warn("Unlock realtime failed postId={}", event.postId(), e);
        }
    }
}
