package com.fanzzi.backend.common.messaging.websocket.model;

import lombok.Data;

@Data
public class AckRequest {
    private String messageId;
}
