package com.fanzzi.backend.post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        log.info("WS Connected: {}", event.getMessage());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info("WS Disconnected: {}", event.getSessionId());
    }
}