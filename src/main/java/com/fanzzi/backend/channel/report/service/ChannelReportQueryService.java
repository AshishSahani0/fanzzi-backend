package com.fanzzi.backend.channel.report.service;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.report.dto.response.ChannelReportResponse;
import com.fanzzi.backend.channel.report.model.ChannelReport;
import com.fanzzi.backend.channel.report.repository.ChannelReportRepository;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.media.gateway.userprofile.UserMediaGateway;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelReportQueryService {

    private final ChannelRepository channelRepo;
    private final ChannelReportRepository reportRepo;
    private final UserRepository userRepo;
    private final UserMediaGateway userMediaGateway;

    // =========================================================
    // OWNER: VIEW REPORTS FOR OWN CHANNEL
    // =========================================================
    /*
     * Allows channel owners to see reports submitted
     * against their channel.
     */
    public List<ChannelReportResponse> getReports(
            String channelId,
            String ownerId
    ) {

        validateOwner(channelId, ownerId);

        List<ChannelReport> reports =
                reportRepo.findByChannelIdOrderByReportedAtDesc(channelId);

        log.debug("Fetched {} reports for channel {}", reports.size(), channelId);

        return mapToResponse(reports);
    }

    // =========================================================
    // OWNER: REPORT COUNT
    // =========================================================
    /*
     * Returns total number of reports for a channel.
     * Useful for showing warning badges in creator UI.
     */
    public long getReportCount(String channelId, String ownerId) {

        validateOwner(channelId, ownerId);

        return reportRepo.countByChannelId(channelId);
    }

    // =========================================================
    // ADMIN: VIEW REPORTS
    // =========================================================
    /*
     * Admin moderation dashboard endpoint.
     * Allows moderators to inspect all reports for a channel.
     */
    public List<ChannelReportResponse> getReportsForAdmin(String channelId) {

        ensureChannelExists(channelId);

        List<ChannelReport> reports =
                reportRepo.findByChannelIdOrderByReportedAtDesc(channelId);

        log.debug("Admin fetched {} reports for channel {}", reports.size(), channelId);

        return mapToResponse(reports);
    }

    // =========================================================
    // MODERATION: CHANNEL TRUST SCORE
    // =========================================================
    /*
     * Calculates total trust score of all reports
     * for a channel.
     *
     * Used by moderation pipeline to decide:
     *
     * warning / hide / ban
     */
    public double getChannelTrustScore(String channelId) {

        ensureChannelExists(channelId);

        Double total = reportRepo.getTotalTrustScore(channelId);

        return total != null ? total : 0.0;
    }

    // =========================================================
    // VALIDATIONS
    // =========================================================

    private void validateOwner(String channelId, String ownerId) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        if (!Objects.equals(channel.getOwnerId(), ownerId)) {

            log.warn(
                    "Unauthorized report access attempt | channel={} user={}",
                    channelId,
                    ownerId
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only channel owner can access reports"
            );
        }
    }

    private void ensureChannelExists(String channelId) {

        if (!channelRepo.existsById(channelId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Channel not found"
            );
        }
    }

    // =========================================================
    // MAPPING: ENTITY → RESPONSE DTO
    // =========================================================

    private List<ChannelReportResponse> mapToResponse(
            List<ChannelReport> reports
    ) {

        if (reports.isEmpty()) {
            return List.of();
        }

        // Collect unique reporter IDs
        List<String> userIds = reports.stream()
                .map(ChannelReport::getReportedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Fetch all users in ONE DB query
        Map<String, User> userMap =
                userRepo.findByIdIn(userIds)
                        .stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                user -> user
                        ));

        return reports.stream()
                .map(report -> buildResponse(report, userMap))
                .toList();
    }

    private ChannelReportResponse buildResponse(
            ChannelReport report,
            Map<String, User> userMap
    ) {

        User user = userMap.get(report.getReportedBy());

        String userName = "User";
        String avatarUrl = null;
        boolean verified = false;

        if (user != null) {

            userName = Optional.ofNullable(user.getUserName())
                    .orElse("User");

            verified = Boolean.TRUE.equals(user.isVerified());

            if (user.getProfileImageKey() != null) {

                avatarUrl = userMediaGateway.getUserProfileUrl(
                        user.getProfileImageKey()
                );
            }
        }

        return ChannelReportResponse.builder()
                .id(report.getId())
                .reportedByUserId(report.getReportedBy())
                .reportedByName(userName)
                .reportedByAvatar(avatarUrl)
                .reporterVerified(verified)
                .reason(report.getReason())
                .description(report.getDescription())
                .evidenceMediaKey(report.getEvidenceMediaKey())
                .reporterTrustScore(report.getReporterTrustScore())
                .reportedAt(report.getReportedAt())
                .build();
    }
}

