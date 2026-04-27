package com.fanzzi.backend.channel.report.service;

import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.report.common.trust.service.UserTrustService;
import com.fanzzi.backend.channel.report.dto.ReportStatus;
import com.fanzzi.backend.channel.report.dto.request.ReportReason;
import com.fanzzi.backend.channel.report.moderation.service.ChannelModerationService;
import com.fanzzi.backend.channel.report.model.ChannelReport;
import com.fanzzi.backend.channel.report.repository.ChannelReportRepository;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelReportService {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ChannelRepository channelRepo;
    private final ChannelMemberRepository memberRepo;
    private final ChannelReportRepository reportRepo;
    private final UserRepository userRepo;
    private final UserTrustService trustService;
    private final ChannelModerationService moderationService;

    // =========================================================
    // 🚩 USER REPORT CHANNEL
    // =========================================================
    /*
     * Flow:
     * USER submits report
     * ↓
     * System validates report
     * ↓
     * Trust score calculated
     * ↓
     * Report stored
     * ↓
     * Moderation pipeline triggered
     */
    @Transactional
    public void reportChannel(
            String channelId,
            String userId,
            ReportReason reason,
            String description,
            String evidenceMediaKey
    ) {

        // Validate report reason
        validateReason(reason);

        // Ensure channel exists
        Channel channel = getChannelOrThrow(channelId);

        // Prevent channel owner from reporting their own channel
        validateNotOwner(channel.getOwnerId(), userId);

        // Only channel members can report
        validateMembership(channelId, userId);

        // Prevent duplicate reports
        validateDuplicateReport(channelId, userId);

        // Fetch reporting user
        User user = getUserOrThrow(userId);

        // Calculate reporter trust score
        double trustScore = trustService.calculateReportWeight(user);

        // Create report record
        ChannelReport report = ChannelReport.builder()
                .channelId(channelId)
                .reportedBy(userId)
                .reason(reason)
                .description(sanitizeDescription(description))
                .evidenceMediaKey(evidenceMediaKey)
                .reporterTrustScore(trustScore)
                .status(ReportStatus.PENDING)
                .build();

        // Save report
        reportRepo.save(report);

        log.info(
                "Channel reported | channelId={} reporter={} reason={} trustScore={}",
                channelId,
                userId,
                reason,
                trustScore
        );

        // =========================================================
        // AUTOMATED MODERATION PIPELINE
        // =========================================================
        /*
         * After each report we trigger moderation evaluation.
         *
         * The moderation service will calculate:
         *
         * totalTrustScore(channel)
         * +
         * reportCount(channel)
         *
         * Based on thresholds the system may:
         *
         * Auto Warning
         * Auto Hide Channel
         * Auto Ban Channel
         *
         * Admin moderators can still override decisions.
         */
        moderationService.updateModerationStatus(channelId);
    }

    // =========================================================
    // 🔒 VALIDATIONS
    // =========================================================

    private void validateReason(ReportReason reason) {
        if (reason == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report reason is required"
            );
        }
    }

    private void validateNotOwner(String ownerId, String userId) {
        if (ownerId.equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Channel owners cannot report their own channel"
            );
        }
    }

    private void validateMembership(String channelId, String userId) {

        boolean isMember = memberRepo.existsByChannelIdAndUserId(channelId, userId);

        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You must join the channel before reporting it"
            );
        }
    }

    private void validateDuplicateReport(String channelId, String userId) {

        boolean alreadyReported =
                reportRepo.existsByChannelIdAndReportedBy(channelId, userId);

        if (alreadyReported) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already reported this channel"
            );
        }
    }

    // =========================================================
    // 🔧 HELPERS
    // =========================================================

    private Channel getChannelOrThrow(String channelId) {

        return channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );
    }

    private User getUserOrThrow(String userId) {

        return userRepo.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );
    }

    /**
     * Sanitizes report description to prevent abuse
     */
    private String sanitizeDescription(String description) {

        if (description == null) {
            return null;
        }

        String sanitized = description.trim();

        if (sanitized.length() > MAX_DESCRIPTION_LENGTH) {
            sanitized = sanitized.substring(0, MAX_DESCRIPTION_LENGTH);
        }

        // remove simple HTML tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        return sanitized.isEmpty() ? null : sanitized;
    }
}

