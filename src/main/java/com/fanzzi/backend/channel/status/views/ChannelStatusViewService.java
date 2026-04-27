package com.fanzzi.backend.channel.status.views;

import com.fanzzi.backend.channel.status.dto.ChannelStatusViewerResponse;
import com.fanzzi.backend.channel.status.model.ChannelStatus;
import com.fanzzi.backend.channel.status.repository.ChannelStatusRepository;
import com.fanzzi.backend.channel.status.service.ChannelStatusMediaService;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.common.messaging.websocket.service.WsSendService;
import com.fanzzi.backend.media.gateway.userprofile.UserMediaGateway;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelStatusViewService {

    private final ChannelStatusRepository statusRepo;
    private final UserRepository userRepo;
    private final StringRedisTemplate redisTemplate;
    private final WsSendService wsSendService;
    private final UserMediaGateway userMediaGateway;

    public void markViewed(String statusId, String viewerId) {

        if (statusId == null || viewerId == null) return;

        ChannelStatus status = statusRepo.findById(statusId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Status not found"));

        if (status.isDeleted() || status.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Status expired");
        }

        String countKey = "status:view:" + statusId;
        String viewerKey = "status:viewers:" + statusId;
        String seenKey = "status:seen:user:" + viewerId;

        try {
            /// 🔥 UNIQUE COUNT
            Long added = redisTemplate.opsForSet().add(countKey, viewerId);

            /// 🔥 ALWAYS UPDATE LAST VIEW TIME
            redisTemplate.opsForZSet().add(
                    viewerKey,
                    viewerId,
                    System.currentTimeMillis()
            );

            /// 🔥 MARK AS SEEN (CRITICAL FIX)
            redisTemplate.opsForSet().add(seenKey, statusId);

            /// 🔥 TTL
            redisTemplate.expire(countKey, Duration.ofHours(48));
            redisTemplate.expire(viewerKey, Duration.ofHours(48));
            redisTemplate.expire(seenKey, Duration.ofHours(48));

            /// 🔥 ONLY SEND EVENT IF NEW VIEW
            if (added != null && added > 0) {

                Long total = redisTemplate.opsForSet().size(countKey);
                long count = total != null ? total : 0;

                Set<String> viewerIds = redisTemplate.opsForZSet()
                        .reverseRange(viewerKey, 0, 9);

                List<Map<String, String>> viewers =
                        buildViewerPreview(viewerIds);

                wsSendService.sendToChannelSubscribers(
                        status.getChannelId(),
                        Map.of(
                                "type", "STATUS_VIEW",
                                "statusId", statusId,
                                "count", count,
                                "viewers", viewers
                        )
                );
            }

        } catch (Exception e) {
            log.error("markViewed failed", e);
        }
    }
    // =====================================================
    // 🔢 VIEW COUNT (REDIS ONLY)
    // =====================================================

    public long getViewCount(String statusId) {

        if (statusId == null) return 0;

        try {
            Long count = redisTemplate.opsForSet()
                    .size("status:view:" + statusId);

            return count != null ? count : 0;

        } catch (Exception e) {
            log.warn("Redis getViewCount failed", e);
            return 0;
        }
    }

    public List<ChannelStatusViewerResponse> getViewers(String statusId) {

        if (statusId == null) return List.of();

        String viewerKey = "status:viewers:" + statusId;

        try {
            Set<String> viewerIds = redisTemplate.opsForZSet()
                    .reverseRange(viewerKey, 0, 100);

            if (viewerIds == null || viewerIds.isEmpty()) return List.of();

            Map<String, User> userMap = userRepo.findAllById(viewerIds)
                    .stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            return viewerIds.stream().map(id -> {

                User user = userMap.get(id);

                String name = "User";
                String avatar = null;

                if (user != null) {
                    name = (user.getFirstName() != null ? user.getFirstName() : "") +
                            (user.getLastName() != null ? " " + user.getLastName() : "");

                    if (name.isBlank()) name = "User";

                    if (user.getProfileImageKey() != null) {
                        try {
                            avatar = userMediaGateway.getUserProfileUrl(
                                    user.getProfileImageKey()
                            );
                        } catch (Exception e) {
                            log.warn("Profile load failed: {}", user.getProfileImageKey());
                        }
                    }
                }

                /// ✅ GET SCORE PER USER (CORRECT)
                Double ts = redisTemplate.opsForZSet().score(viewerKey, id);

                return ChannelStatusViewerResponse.builder()
                        .viewerId(id)
                        .viewerName(name.trim())
                        .viewerProfile(avatar)
                        .viewedAt(ts != null
                                ? Instant.ofEpochMilli(ts.longValue())
                                : null)
                        .build();

            }).toList();

        } catch (Exception e) {
            log.warn("getViewers failed", e);
            return List.of();
        }
    }
    // =====================================================
    // 🧠 HELPERS
    // =====================================================

    private List<Map<String, String>> buildViewerPreview(Set<String> viewerIds) {

        if (viewerIds == null || viewerIds.isEmpty()) return List.of();

        Map<String, User> userMap = userRepo.findAllById(viewerIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return viewerIds.stream().map(id -> {

            User user = userMap.get(id);

            String name = "User";
            String avatar = null;

            if (user != null) {
                name = (user.getFirstName() != null ? user.getFirstName() : "") +
                        (user.getLastName() != null ? " " + user.getLastName() : "");

                if (name.isBlank()) name = "User";

                if (user.getProfileImageKey() != null) {
                    try {
                        avatar = userMediaGateway.getUserProfileUrl(
                                user.getProfileImageKey()
                        );
                    } catch (Exception e) {
                        log.warn("Preview profile load failed: {}", user.getProfileImageKey());
                    }
                }
            }

            return Map.of(
                    "viewerId", id,
                    "name", name.trim(),
                    "profile", avatar
            );

        }).toList();
    }

    private List<ChannelStatusViewerResponse> buildViewerResponse(Set<String> viewerIds) {

        Map<String, User> userMap = userRepo.findAllById(viewerIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return viewerIds.stream().map(id -> {

            User user = userMap.get(id);

            String name = "User";
            String avatar = null;

            if (user != null) {
                name = (user.getFirstName() != null ? user.getFirstName() : "") +
                        (user.getLastName() != null ? " " + user.getLastName() : "");

                if (name.isBlank()) name = "User";

                if (user.getProfileImageKey() != null) {
                    try {
                        avatar = userMediaGateway.getUserProfileUrl(
                                user.getProfileImageKey()
                        );
                    } catch (Exception e) {
                        log.warn("Viewer profile load failed: {}", user.getProfileImageKey());
                    }
                }
            }

            return ChannelStatusViewerResponse.builder()
                    .viewerId(id)
                    .viewerName(name.trim())
                    .viewerProfile(avatar)
                    .viewedAt(null) // optional
                    .build();

        }).toList();
    }
}