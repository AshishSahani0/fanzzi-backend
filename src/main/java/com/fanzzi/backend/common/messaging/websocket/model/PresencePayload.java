package com.fanzzi.backend.common.messaging.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresencePayload {
    private String userId;
    private boolean online;
}