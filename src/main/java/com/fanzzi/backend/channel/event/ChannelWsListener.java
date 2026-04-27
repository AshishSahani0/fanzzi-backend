package com.fanzzi.backend.channel.event;

import com.fanzzi.backend.common.messaging.websocket.model.WsEvent;
import com.fanzzi.backend.common.messaging.websocket.model.WsMessageType;
import com.fanzzi.backend.common.messaging.websocket.service.WsSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelWsListener {

    private final WsSendService wsSendService;

    @EventListener
    public void handle(ChannelEvent event) {

        try {
            if (event == null || event.getChannelId() == null) {
                return;
            }

            String channelId = event.getChannelId();

            WsMessageType type = mapType(event.getType());


            WsEvent<Object> wsEvent = new WsEvent<>(
                    type.name(),
                    channelId,
                    event.getPayload()
            );


            wsSendService.sendChannelEvent(channelId, wsEvent);

            if (event.getUserId() != null) {
                wsSendService.sendToUser(
                        event.getUserId(),
                        wsEvent
                );
            }



        } catch (Exception e) {

        }
    }

    private WsMessageType mapType(ChannelEventType type) {

        if (type == null) return WsMessageType.CHANNEL_UPDATE;

        return switch (type) {
            case CREATE -> WsMessageType.CHANNEL_CREATE;
            case UPDATE -> WsMessageType.CHANNEL_UPDATE;
            case JOIN -> WsMessageType.CHANNEL_JOIN;
            case LEAVE -> WsMessageType.CHANNEL_LEAVE;
            case DELETE -> WsMessageType.CHANNEL_DELETE;
            case RESTORE -> WsMessageType.CHANNEL_RESTORE;
            case MEMBER_COUNT_UPDATE -> WsMessageType.CHANNEL_UPDATE;

        };
    }
}