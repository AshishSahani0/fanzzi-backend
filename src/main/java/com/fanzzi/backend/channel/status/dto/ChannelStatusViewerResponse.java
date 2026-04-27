package com.fanzzi.backend.channel.status.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelStatusViewerResponse {

    private String viewerId;

    private String viewerName;

    private String viewerProfile;

    private Instant viewedAt;
}