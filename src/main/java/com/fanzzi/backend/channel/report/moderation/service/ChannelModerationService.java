package com.fanzzi.backend.channel.report.moderation.service;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.report.service.ChannelReportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChannelModerationService {

    private final ChannelRepository channelRepo;
    private final ChannelReportQueryService reportQueryService;

    // =====================================================
    // ⭐ AUTO MODERATION THRESHOLDS
    // =====================================================
    /*
     * These thresholds determine how the system reacts
     * to accumulated report trust scores.
     *
     * Example:
     * Verified user report = 2.0
     * Normal user report   = 1.0
     * New user report      = 0.5
     */

    private static final double WARNING_THRESHOLD = 3.0;
    private static final double REVIEW_THRESHOLD = 6.0;
    private static final double RESTRICT_THRESHOLD = 10.0;

    // =====================================================
    // 🔧 HELPER
    // =====================================================

    private Channel getChannelOrThrow(String channelId) {
        return channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );
    }

    // =====================================================
    // 🧠 AUTO MODERATION PIPELINE
    // =====================================================
    /*
     * Called whenever a new report is submitted.
     *
     * Flow:
     * Report submitted
     * ↓
     * Trust score recalculated
     * ↓
     * Channel risk evaluated
     * ↓
     * Moderation status updated
     */
    public ChannelModerationStatus updateModerationStatus(String channelId) {

        Channel channel = getChannelOrThrow(channelId);

        double score = reportQueryService.getChannelTrustScore(channelId);

        ChannelModerationStatus newStatus = calculateStatus(score);

        ChannelModerationStatus currentStatus =
                channel.getModerationStatus() != null
                        ? channel.getModerationStatus()
                        : ChannelModerationStatus.NORMAL;

        // Avoid unnecessary DB writes
        if (currentStatus != newStatus) {
            channel.setModerationStatus(newStatus);
            channelRepo.save(channel);
        }

        return newStatus;
    }

    // =====================================================
    // 🧮 STATUS CALCULATION
    // =====================================================

    private ChannelModerationStatus calculateStatus(double score) {

        if (score >= RESTRICT_THRESHOLD) {
            return ChannelModerationStatus.RESTRICTED;
        }

        if (score >= REVIEW_THRESHOLD) {
            return ChannelModerationStatus.UNDER_REVIEW;
        }

        if (score >= WARNING_THRESHOLD) {
            return ChannelModerationStatus.WARNING;
        }

        return ChannelModerationStatus.NORMAL;
    }
}

