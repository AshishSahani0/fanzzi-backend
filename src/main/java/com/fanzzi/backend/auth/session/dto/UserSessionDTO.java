package com.fanzzi.backend.auth.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDTO {

    private String userId;

    private String deviceId;

    private boolean active;
    private boolean banned;
    private boolean deleted;

    private String fingerprint;

    private String ipAddress;
    private Instant lastIpChangeAt;
    private String userAgent;

    private String sessionId;

    private Instant lastAccessAt;
}