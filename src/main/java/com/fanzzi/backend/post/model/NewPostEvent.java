package com.fanzzi.backend.post.model;

import com.fanzzi.backend.post.enums.EventType;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewPostEvent {

    // =====================================
    // 🔥 EVENT TYPE
    // =====================================
    private EventType type; // POST_CREATED

    // =====================================
    // 📺 CONTEXT
    // =====================================
    private String channelId;
    private String postId;

    private long seq;

    // =====================================
    // ⏱ TIME
    // =====================================
    private Instant createdAt;

    // =====================================
    // ⚡ OPTIONAL (LIGHT PAYLOAD)
    // =====================================
    private boolean hasMedia;
    private boolean isPaid;
}