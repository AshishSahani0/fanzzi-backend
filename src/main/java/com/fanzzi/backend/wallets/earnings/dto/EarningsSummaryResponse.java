package com.fanzzi.backend.wallets.earnings.dto;

import lombok.Builder;

@Builder
public record EarningsSummaryResponse(
        long totalEarned,
        long monthlyEarned,
        long available
) {}