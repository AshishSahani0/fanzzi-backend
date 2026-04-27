package com.fanzzi.backend.common.messaging.websocket.handler;

import com.fanzzi.backend.common.messaging.websocket.service.WsPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketDisconnectListener {

    private final WsPresenceService presenceService;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {

        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();

            if (sessionAttrs == null) return;

            String userId = (String) sessionAttrs.get("userId");

            if (userId == null) return;

            // =====================================================
            // 🔴 SAFE OFFLINE UPDATE
            // =====================================================
            presenceService.setOffline(userId);

        } catch (Exception e) {
            // 🔥 NEVER break WS thread
            // optional: log.error("Disconnect error", e);
        }
    }
}