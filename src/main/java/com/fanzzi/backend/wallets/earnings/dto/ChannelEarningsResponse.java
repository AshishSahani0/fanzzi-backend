package com.fanzzi.backend.wallets.earnings.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record ChannelEarningsResponse(
        Map<String, Long> channels
) {}