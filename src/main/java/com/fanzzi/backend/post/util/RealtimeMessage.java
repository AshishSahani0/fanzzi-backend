package com.fanzzi.backend.post.util;

import com.fanzzi.backend.post.enums.EventType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RealtimeMessage<T> {

    private EventType type;
    private String channelId;
    private long seq;
    private long timestamp;
    private T data;
}