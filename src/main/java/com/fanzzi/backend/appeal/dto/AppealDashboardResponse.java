package com.fanzzi.backend.appeal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AppealDashboardResponse {

    private String appealId;
    private String userId;
    private String phone;

    private String banReason;
    private Instant bannedAt;

    private String appealMessage;
    private String status;

    private Instant createdAt;
}
