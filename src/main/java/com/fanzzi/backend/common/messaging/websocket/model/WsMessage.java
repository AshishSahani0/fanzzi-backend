package com.fanzzi.backend.common.messaging.websocket.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsMessage<T> {

    private String messageId;
    private WsMessageType type;
    private T payload;
    private long timestamp;
}