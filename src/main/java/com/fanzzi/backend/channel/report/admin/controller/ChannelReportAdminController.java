package com.fanzzi.backend.channel.report.admin.controller;


import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.report.admin.controller.ChannelAdminModerationService;
import com.fanzzi.backend.channel.report.dto.response.ChannelReportResponse;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;

import com.fanzzi.backend.channel.report.service.ChannelReportQueryService;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/channels")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ChannelReportAdminController {

    private final ChannelReportQueryService queryService;
    private final ChannelAdminModerationService moderationService;
    private final ChannelRepository channelRepo;

    // =====================================================
    // 👀 ADMIN: VIEW REPORTS FOR CHANNEL
    // =====================================================
    /*
     * Returns all reports submitted against a channel.
     *
     * Used in admin moderation dashboard.
     *
     * Endpoint:
     * GET /api/admin/channels/{channelId}/reports
     */
    @GetMapping("/{channelId}/reports")
    public List<ChannelReportResponse> getReportsForChannel(
            @PathVariable String channelId
    ) {
        return queryService.getReportsForAdmin(channelId);
    }

    // =====================================================
    // ⭐ ADMIN: CHANNEL TRUST SCORE
    // =====================================================
    /*
     * Returns total trust score accumulated
     * from channel reports.
     *
     * This helps moderators understand how
     * serious the abuse level is.
     */
    @GetMapping("/{channelId}/reports/score")
    public Map<String, Double> getScore(
            @PathVariable String channelId
    ) {
        double score = queryService.getChannelTrustScore(channelId);
        return Map.of("trustScore", score);
    }

    // =====================================================
    // 🛡 ADMIN: CURRENT MODERATION STATUS
    // =====================================================

    @GetMapping("/{channelId}/status")
    public ChannelModerationStatus getStatus(
            @PathVariable String channelId
    ) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        return channel.getModerationStatus() != null
                ? channel.getModerationStatus()
                : ChannelModerationStatus.NORMAL;
    }

    // =====================================================
    // ⚙️ ADMIN: SET MODERATION STATUS
    // =====================================================
    /*
     * Allows moderators to manually override
     * moderation decisions.
     *
     * Example:
     * /status?status=RESTRICTED
     */
    @PutMapping("/{channelId}/status")
    public ChannelModerationStatus setStatus(
            @PathVariable String channelId,
            @RequestParam ChannelModerationStatus status
    ) {
        return moderationService.setStatus(channelId, status);
    }

    // =====================================================
    // 🚫 ADMIN: RESTRICT CHANNEL
    // =====================================================

    @PostMapping("/{channelId}/restrict")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> restrictChannel(
            @PathVariable String channelId
    ) {

        moderationService.restrictChannel(channelId);

        return Map.of("message", "Channel restricted");
    }

    // =====================================================
    // ✅ ADMIN: RESTORE CHANNEL
    // =====================================================

    @PostMapping("/{channelId}/restore")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> restoreChannel(
            @PathVariable String channelId
    ) {

        moderationService.restoreChannel(channelId);

        return Map.of("message", "Channel restored");
    }

    // =====================================================
    // 🧹 ADMIN: CLEAR REPORT HISTORY
    // =====================================================
    /*
     * Deletes all reports associated with a channel.
     *
     * Useful when:
     * - reports were spam
     * - channel issue resolved
     */
    @DeleteMapping("/{channelId}/reports")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> clearReports(
            @PathVariable String channelId
    ) {

        moderationService.clearReports(channelId);

        return Map.of("message", "Reports cleared");
    }
}

