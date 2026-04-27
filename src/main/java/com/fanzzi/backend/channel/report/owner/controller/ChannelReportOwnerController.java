package com.fanzzi.backend.channel.report.owner.controller;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.report.dto.response.ChannelReportCountResponse;
import com.fanzzi.backend.channel.report.dto.response.ChannelReportResponse;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;
import com.fanzzi.backend.channel.report.service.ChannelReportQueryService;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/channels/{channelId}/reports")
@RequiredArgsConstructor
public class ChannelReportOwnerController {

    private final ChannelReportQueryService queryService;
    private final ChannelRepository channelRepo;

    // =====================================================
    // 🔒 OWNER VALIDATION
    // =====================================================
    /*
     * Ensures the requesting user is the channel owner.
     * Used for all owner-specific report endpoints.
     */
    private Channel validateOwner(String channelId, String ownerId) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        if (!ownerId.equals(channel.getOwnerId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only channel owner allowed"
            );
        }

        return channel;
    }

    // =====================================================
    // 👀 OWNER: VIEW REPORT LIST
    // =====================================================
    /*
     * Allows channel owner to see all reports
     * submitted against their channel.
     *
     * Endpoint:
     * GET /api/channels/{channelId}/reports
     */
    @GetMapping
    public List<ChannelReportResponse> getReports(
            @PathVariable String channelId
    ) {

        String ownerId = SecurityUtil.getCurrentUserId();

        return queryService.getReports(channelId, ownerId);
    }

    // =====================================================
    // 🔢 OWNER: REPORT COUNT
    // =====================================================
    /*
     * Returns total number of reports for a channel.
     *
     * Useful for creator dashboards showing
     * warning indicators.
     *
     * Endpoint:
     * GET /api/channels/{channelId}/reports/count
     */
    @GetMapping("/count")
    public ChannelReportCountResponse getReportCount(
            @PathVariable String channelId
    ) {

        String ownerId = SecurityUtil.getCurrentUserId();

        long count = queryService.getReportCount(channelId, ownerId);

        return new ChannelReportCountResponse(count);
    }

    // =====================================================
    // ⭐ OWNER: TRUST SCORE
    // =====================================================
    /*
     * Returns the total trust score of all reports
     * against the channel.
     *
     * This value is used internally by the moderation
     * system to decide whether to:
     *
     * - Warn the channel
     * - Hide the channel
     * - Ban the channel
     *
     * Endpoint:
     * GET /api/channels/{channelId}/reports/score
     */
    @GetMapping("/score")
    public double getTrustScore(@PathVariable String channelId) {

        String ownerId = SecurityUtil.getCurrentUserId();

        validateOwner(channelId, ownerId);

        return queryService.getChannelTrustScore(channelId);
    }

    // =====================================================
    // 🛡 OWNER: MODERATION STATUS
    // =====================================================
    /*
     * Returns the moderation status of the channel.
     *
     * Possible values:
     * NORMAL
     * WARNING
     * HIDDEN
     * BANNED
     *
     * Endpoint:
     * GET /api/channels/{channelId}/reports/moderation-status
     */
    @GetMapping("/moderation-status")
    public ChannelModerationStatus getStatus(
            @PathVariable String channelId
    ) {

        String ownerId = SecurityUtil.getCurrentUserId();

        Channel channel = validateOwner(channelId, ownerId);

        // Null-safe for legacy channels
        return channel.getModerationStatus() != null
                ? channel.getModerationStatus()
                : ChannelModerationStatus.NORMAL;
    }
}

