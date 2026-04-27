package com.fanzzi.backend.common.messaging.websocket.model;


public enum WsMessageType {
    CHAT_MESSAGE,
    CHANNEL_CREATE,
    CHANNEL_UPDATE,
    CHANNEL_JOIN,
    CHANNEL_LEAVE,
    CHANNEL_RESTORE,
    USER_EVENT,
    SYSTEM_EVENT,
    MESSAGE_STATUS,
    CHANNEL_DELETE,
}
