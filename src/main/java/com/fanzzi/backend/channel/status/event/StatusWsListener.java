package com.fanzzi.backend.channel.status.event;

import com.fanzzi.backend.channel.status.event.StatusEvent;
import com.fanzzi.backend.common.messaging.websocket.model.WsEvent;
import com.fanzzi.backend.common.messaging.websocket.service.WsSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusWsListener {

    private final WsSendService wsSendService;

    @EventListener
    public void handle(StatusEvent event) {

        if (event == null || event.getChannelId() == null) return;

        WsEvent<Object> wsEvent = new WsEvent<>(
                "STATUS_" + event.getType().name(),
                event.getChannelId(),
                event.getPayload()
        );

        // ✅ ONLY send to subscribers (CRITICAL)
        wsSendService.sendToChannelSubscribers(
                event.getChannelId(),
                wsEvent
        );
    }
}