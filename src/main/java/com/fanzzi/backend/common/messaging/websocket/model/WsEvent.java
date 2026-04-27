package com.fanzzi.backend.common.messaging.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WsEvent<T> {

    private String event;      // CHANNEL_JOIN
    private String channelId;  // important for frontend routing
    private T data;            // actual payload
}