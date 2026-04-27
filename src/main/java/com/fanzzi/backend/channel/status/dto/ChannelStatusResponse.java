package com.fanzzi.backend.channel.status.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelStatusResponse {

    private String id;
    private String type;
    private String text;
    private List<StatusMedia> media;

    private List<String> mediaUrls;

    private Instant createdAt;
    private Instant expiresAt;

    // NEW
    private long viewCount;

    private List<String> links;
    private List<String> hashtags;
    private List<String> mentions;
    private boolean isText;

    private String backgroundColor;

    // first few viewers
    private List<ChannelStatusViewerResponse> viewerPreview;

    private boolean isUnseen;
}