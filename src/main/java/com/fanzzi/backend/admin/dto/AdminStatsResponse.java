package com.fanzzi.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminStatsResponse {

    private long totalUsers;
    private long activeUsers;
    private long realUsers;

    private long pendingAppeals;
    private long acceptedAppeals;
    private long rejectedAppeals;

    private long bannedUsers;
}
