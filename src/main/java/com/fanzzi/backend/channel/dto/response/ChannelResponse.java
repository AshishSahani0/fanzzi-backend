package com.fanzzi.backend.channel.dto.response;

import com.fanzzi.backend.channel.enums.ChannelType;
import com.fanzzi.backend.channel.enums.ChannelVisibility;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor   // 🔥 VERY IMPORTANT
@AllArgsConstructor
@Builder
public class ChannelResponse {

    // ==========================================
    // 🆔 BASIC
    // ==========================================

    private String id;
    private String name;
    private String description;

    // ==========================================
    // 🖼 PROFILE
    // ==========================================

    private String profileImageKey;
    private String profileImageUrl;

    // ==========================================
    // 🔐 VISIBILITY & TYPE
    // ==========================================

    private ChannelVisibility visibility;
    private ChannelType type;
    private Long monthlyPrice;

    // ==========================================
    // 🔗 ACCESS
    // ==========================================

    private String slug;
    private String inviteToken;
    private String inviteLink;

    // ==========================================
    // 👥 COUNTERS (CRITICAL FOR UI)
    // ==========================================

    private long memberCount;
    private long subscriberCount;
    private long postCount;

    // ==========================================
    // 🧠 ACCESS FLAGS
    // ==========================================

    private boolean owner;
    private boolean member;
    private boolean subscribed;

    private boolean canRead;
    private boolean blurred;
    private boolean canPost;

    private boolean hasActiveStatus;
    private boolean joined;

    private String category;
    private String language;
    private Boolean discoverable;
    private Boolean nsfw;

    private boolean blocked;

    // ==========================================
    // ⏱ ACTIVITY
    // ==========================================

    private Instant lastPostAt;
    private Instant createdAt;
}