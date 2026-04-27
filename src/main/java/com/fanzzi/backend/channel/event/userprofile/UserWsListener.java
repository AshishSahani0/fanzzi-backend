package com.fanzzi.backend.channel.event.userprofile;

import com.fanzzi.backend.common.messaging.websocket.model.WsEvent;
import com.fanzzi.backend.common.messaging.websocket.service.WsSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserWsListener {

    private final WsSendService wsSendService;

    @EventListener
    public void handle(UserEvent event) {

        try {
            if (event == null || event.getUserId() == null) return;

            WsEvent<Object> wsEvent = new WsEvent<>(
                    event.getType().name(),
                    event.getUserId(),
                    event.getPayload()
            );


            wsSendService.sendToUser(event.getUserId(), wsEvent);



        } catch (Exception e) {

        }
    }
}
