package com.fanzzi.backend.channel.status.service;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.status.dto.*;
import com.fanzzi.backend.channel.status.model.ChannelStatus;
import com.fanzzi.backend.channel.status.repository.ChannelStatusAggregationRepository;
import com.fanzzi.backend.channel.status.repository.ChannelStatusRepository;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.common.messaging.websocket.service.WsSendService;
import com.fanzzi.backend.media.gateway.userprofile.UserMediaGateway;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelStatusService {

    private static final int MAX_MEDIA = 5;
    private static final long MAX_DURATION_SECONDS = 60; // 1 minute
    private static final long STATUS_LIFETIME_SECONDS = 86400; // 24h

    private final ChannelStatusRepository repo;
    private final ChannelStatusMediaService mediaService;
    private final ChannelRepository channelRepo;
    private final UserRepository userRepo;
    private final UserMediaGateway userMediaGateway;


    private final ChannelStatusAggregationRepository aggregationRepo;
    private final StringRedisTemplate redisTemplate;
    private final WsSendService wsSendService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChannelStatusResponse create(String channelId, String userId, CreateChannelStatusRequest request) {

        if (channelId == null || userId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid input");
        }

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Channel not found"));

        if (!channel.getOwnerId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only owner can post status");
        }

        validate(request, channelId);

        Instant now = Instant.now();
        EnrichedText enriched = enrichText(request.getText());
        List<StatusMedia> media = convertMedia(request.getMedia());

        String previewKey = null;

        if (media != null && !media.isEmpty()) {
            previewKey = media.stream()
                    .filter(m -> m.getMediaType() == MediaType.IMAGE) // ✅ FIXED
                    .map(StatusMedia::getMediaKey)
                    .findFirst()
                    .orElse(media.get(0).getMediaKey());
        }

        ChannelStatus status = ChannelStatus.builder()
                .channelId(channelId)
                .ownerId(userId)
                .type(
                        (media != null && !media.isEmpty())
                                ? StatusType.MEDIA
                                : StatusType.TEXT
                )
                .text(enriched != null ? enriched.getText() : null)
                .backgroundColor(
                        request.getBackgroundColor() != null
                                ? request.getBackgroundColor()
                                : "#FF000000" // 🔥 default black
                )
                .media(media != null ? media : List.of())
                .createdAt(now)
                .expiresAt(now.plusSeconds(STATUS_LIFETIME_SECONDS))
                .deleted(false)
                .viewCount(0)
                .previewMediaKey(previewKey)
                .bucket(now.toString().substring(0, 10))
                .build();

        repo.save(status);

        // ================= REDIS CACHE =================
        try {
            String key = "status:channel:" + channelId;

            ChannelStatusResponse response = ChannelStatusResponse.builder()
                    .id(status.getId())
                    .type(status.getType().name())
                    .text(status.getText())
                    .media(status.getMedia())
                    .mediaUrls(buildMediaUrls(status))
                    .createdAt(status.getCreatedAt())
                    .expiresAt(status.getExpiresAt())
                    .viewCount(0)
                    .isText(status.getType() == StatusType.TEXT)
                    .backgroundColor(status.getBackgroundColor())
                    .isUnseen(true) // 🔥 new status always unseen
                    .build();

            String json = objectMapper.writeValueAsString(response);

            redisTemplate.opsForList().leftPush(key, json);
            redisTemplate.expire(key, Duration.ofHours(24));

        } catch (Exception e) {
            log.warn("Redis cache push failed", e);
        }


        // ================= REALTIME =================
        try {
            Map<String, Object> payload = new HashMap<>();

            payload.put("type", "STATUS_CREATE");
            payload.put("statusId", status.getId());
            payload.put("channelId", channelId);
            payload.put("createdAt", now.toString());

            if (status.getBackgroundColor() != null) {
                payload.put("backgroundColor", status.getBackgroundColor());
            }

            // ✅ ONLY ADD IF NOT NULL
            if (previewKey != null) {
                payload.put("preview", previewKey);
            }

            wsSendService.sendToChannelSubscribers(channelId, payload);

        } catch (Exception e) {
            log.warn("WS push failed", e);
        }

        return ChannelStatusResponse.builder()
                .id(status.getId())
                .type(status.getType().name())
                .text(status.getText())
                .media(status.getMedia())
                .mediaUrls(buildMediaUrls(status))
                .createdAt(status.getCreatedAt())
                .expiresAt(status.getExpiresAt())
                .viewCount(0)
                .isText(status.getType() == StatusType.TEXT)
                .backgroundColor(status.getBackgroundColor())
                .isUnseen(true)
                .build();
    }


    private void validate(CreateChannelStatusRequest request, String channelId) {

        if (request == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Request cannot be null");
        }

        if (channelId == null || channelId.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid channel");
        }

        String text = request.getText();
        List<CreateChannelStatusRequest.StatusMediaRequest> media = request.getMedia();

        // ================= TEXT =================
        if (text != null && text.length() > 2000) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Text too long");
        }

        // ================= MEDIA =================
        if (media != null && !media.isEmpty()) {

            if (media.size() > MAX_MEDIA) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Max " + MAX_MEDIA + " media allowed");
            }

            Set<String> uniqueKeys = new HashSet<>();
            long totalSize = 0;

            for (CreateChannelStatusRequest.StatusMediaRequest m : media) {

                if (m == null || m.getMediaKey() == null) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid media");
                }

                String key = m.getMediaKey();

                // 🔐 SECURITY
                if (!key.startsWith("status/channels/" + channelId + "/")) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid media key");
                }

                // ❌ duplicate
                if (!uniqueKeys.add(key)) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "Duplicate media");
                }

                String lower = key.toLowerCase();

                boolean isImage = lower.endsWith(".jpg")
                        || lower.endsWith(".jpeg")
                        || lower.endsWith(".png")
                        || lower.endsWith(".webp");

                boolean isVideo = lower.endsWith(".mp4");
                boolean isAudio = lower.endsWith(".mp3") || lower.endsWith(".aac");

                if (!isImage && !isVideo && !isAudio) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "Unsupported media type");
                }

                // 🎥 video validation
                if (isVideo) {
                    if (m.getDuration() == null || m.getDuration() <= 0 || m.getDuration() > 60) {
                        throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid video duration");
                    }
                }

                totalSize += m.getSize() != null ? m.getSize() : 0;
            }

            if (totalSize > 20 * 1024 * 1024) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Total size exceeds 20MB");
            }
        }

        // 🎨 VALIDATE COLOR
        if (request.getBackgroundColor() != null) {
            String color = request.getBackgroundColor();

            if (!color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid color format");
            }
        }

        // ❌ EMPTY STATUS NOT ALLOWED
        if ((text == null || text.isBlank()) && (media == null || media.isEmpty())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Empty status not allowed");
        }
    }

    private EnrichedText enrichText(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        // 🔥 LIMIT TEXT SIZE (safety)
        if (text.length() > 5000) {
            text = text.substring(0, 5000);
        }

        // ================= REGEX =================

        Pattern urlPattern = Pattern.compile(
                "(https?://\\S+)",
                Pattern.CASE_INSENSITIVE
        );

        Pattern hashtagPattern = Pattern.compile(
                "(?<!\\w)#(\\w+)"
        );

        Pattern mentionPattern = Pattern.compile(
                "(?<!\\w)@(\\w+)"
        );

        // ================= EXTRACTION =================

        Set<String> links = extractMatchesSet(urlPattern, text, 10);
        Set<String> hashtags = extractMatchesSet(hashtagPattern, text, 20);
        Set<String> mentions = extractMatchesSet(mentionPattern, text, 20);

        // ================= NORMALIZATION =================

        List<String> normalizedLinks = links.stream()
                .map(this::cleanUrl)
                .toList();

        List<String> normalizedHashtags = hashtags.stream()
                .map(String::toLowerCase)
                .toList();

        List<String> normalizedMentions = mentions.stream()
                .map(String::toLowerCase)
                .toList();

        return EnrichedText.builder()
                .text(text)
                .links(normalizedLinks)
                .hashtags(normalizedHashtags)
                .mentions(normalizedMentions)
                .build();
    }

    private Set<String> extractMatchesSet(
            Pattern pattern,
            String text,
            int limit
    ) {
        Set<String> results = new LinkedHashSet<>();

        Matcher matcher = pattern.matcher(text);

        while (matcher.find() && results.size() < limit) {
            results.add(matcher.group());
        }

        return results;
    }
    private String cleanUrl(String url) {

        if (url == null) return null;

        // 🔥 REMOVE TRAILING SYMBOLS
        return url.replaceAll("[.,!?]+$", "");
    }


    private List<StatusMedia> convertMedia(
            List<CreateChannelStatusRequest.StatusMediaRequest> media
    ) {

        if (media == null || media.isEmpty()) {
            return List.of();
        }

        Set<String> seenKeys = new HashSet<>();

        return media.stream()
                .filter(Objects::nonNull)

                // 🔥 VALID KEY
                .filter(m -> m.getMediaKey() != null && !m.getMediaKey().isBlank())

                // 🔥 TRIM + CLEAN
                .map(m -> {
                    String key = m.getMediaKey().trim();

                    return StatusMedia.builder()
                            .mediaType(m.getMediaType())
                            .mediaKey(key)
                            .duration(
                                    m.getDuration() != null && m.getDuration() > 0
                                            ? m.getDuration()
                                            : null
                            )
                            .build();
                })

                // 🔥 REMOVE DUPLICATES
                .filter(m -> seenKeys.add(m.getMediaKey()))

                // 🔥 LIMIT (extra safety)
                .limit(MAX_MEDIA)

                .toList();
    }



    public void validateOwnerFast(String channelId, String userId) {

        if (channelId == null || userId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid input");
        }

        boolean isOwner = channelRepo.existsByIdAndOwnerId(channelId, userId);

        if (!isOwner) {
            throw new ApiException(
                    ErrorCode.FORBIDDEN,
                    "Only channel owner allowed"
            );
        }
    }

    public List<ChannelStatusResponse> getActiveStatuses(
            String channelId,
            String userId,
            int page,
            int size
    ) {

        if (channelId == null || channelId.isBlank()) return List.of();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        // ================= ACCESS CONTROL =================
        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Channel not found"));

        boolean isOwner = channel.getOwnerId().equals(userId);

        // 👉 YOU CAN EXTEND THIS LATER (member/subscriber logic)


        String redisKey = "status:channel:" + channelId;

        Set<String> seenSet = getSeenStatusIds(userId);

        // ================= REDIS =================
        try {
            List<String> cached = redisTemplate.opsForList().range(
                    redisKey,
                    (long) safePage * safeSize,
                    (long) (safePage + 1) * safeSize - 1
            );

            if (cached != null && !cached.isEmpty()) {
                return cached.stream()
                        .map(json -> {
                            try {
                                ChannelStatusResponse res =
                                        objectMapper.readValue(json, ChannelStatusResponse.class);
                                // 🔥 OWNER ONLY ENRICHMENT
                                if (isOwner) {
                                    res.setViewCount(safeViewCount(res.getId()));
                                    res.setViewerPreview(getViewerPreview(res.getId())); // 🔥 ADD THIS
                                }

                                res.setUnseen(!seenSet.contains(res.getId()));

                                return res;
                            } catch (Exception e) {
                                log.warn("Redis parse failed: {}", json, e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList();
            }

        } catch (Exception e) {
            log.warn("Redis fetch failed", e);
        }

        // ================= DB =================
        List<ChannelStatus> list =
                repo.findByChannelIdAndDeletedFalseAndExpiresAtAfter(
                        channelId,
                        Instant.now(),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );

        if (list.isEmpty()) return List.of();

        List<ChannelStatusResponse> response = list.stream()
                .skip((long) safePage * safeSize)
                .limit(safeSize)
                .map(status -> {

                    long viewCount = isOwner
                            ? safeViewCount(status.getId()) // 🔥 LIVE COUNT
                            : 0;

                    List<ChannelStatusViewerResponse> preview = isOwner
                            ? getViewerPreview(status.getId())
                            : List.of();

                    return ChannelStatusResponse.builder()
                            .id(status.getId())
                            .type(status.getType().name())
                            .text(status.getText())
                            .media(status.getMedia())
                            .mediaUrls(buildMediaUrls(status))
                            .createdAt(status.getCreatedAt())
                            .expiresAt(status.getExpiresAt())
                            .viewCount(viewCount)
                            .viewerPreview(preview) // 🔥 ADD THIS
                            .isText(status.getType() == StatusType.TEXT)
                            .backgroundColor(status.getBackgroundColor())
                            .isUnseen(!seenSet.contains(status.getId()))
                            .build();
                })
                .toList();

        // ================= CACHE =================
        try {
            List<String> jsonList = response.stream()
                    .map(r -> {
                        try {
                            return objectMapper.writeValueAsString(r);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!jsonList.isEmpty()) {
                redisTemplate.opsForList().leftPushAll(redisKey, jsonList);
                redisTemplate.expire(redisKey, Duration.ofHours(24));
            }

        } catch (Exception e) {
            log.warn("Redis cache failed", e);
        }

        return response;
    }

    private List<ChannelStatusViewerResponse> getViewerPreview(String statusId) {

        try {
            String viewerKey = "status:viewers:" + statusId;

            Set<String> viewerIds = redisTemplate.opsForZSet()
                    .reverseRange(viewerKey, 0, 5);

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

                return ChannelStatusViewerResponse.builder()
                        .viewerId(id)
                        .viewerName(name.trim())
                        .viewerProfile(avatar)
                        .viewedAt(null) // preview → no need timestamp
                        .build();

            }).toList();

        } catch (Exception e) {
            log.warn("getViewerPreview failed", e);
            return List.of();
        }
    }

    private List<String> buildMediaUrls(ChannelStatus status) {

        if (status.getMedia() == null || status.getMedia().isEmpty()) {
            return List.of();
        }

        List<String> keys = status.getMedia().stream()
                .map(StatusMedia::getMediaKey)
                .toList();

        Map<String, String> map = mediaService.resolvePublicUrls(keys);

        return keys.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .toList();
    }



    private long safeViewCount(String statusId) {
        try {
            Long count = redisTemplate.opsForSet()
                    .size("status:view:" + statusId);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public void deleteStatus(String channelId, String statusId, String userId) {

        if (channelId == null || statusId == null || userId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid input");
        }

        ChannelStatus status = repo.findById(statusId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Status not found"));

        // =====================================================
        // 🔐 VALIDATION
        // =====================================================
        if (!status.getChannelId().equals(channelId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Invalid channel");
        }

        if (!safeEquals(status.getOwnerId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not allowed");
        }

        // 🔥 ALREADY DELETED (idempotent)
        if (status.isDeleted()) return;

        // =====================================================
        // 🗑 SOFT DELETE
        // =====================================================
        status.setDeleted(true);
        status.setExpiresAt(Instant.now()); // 🔥 expire immediately
        repo.save(status);

        // =====================================================
        // 🧹 REDIS CLEANUP (SAFE JSON PARSE)
        // =====================================================
        try {
            String redisKey = "status:channel:" + channelId;

            List<String> cached = redisTemplate.opsForList().range(redisKey, 0, -1);

            if (cached != null && !cached.isEmpty()) {

                List<String> updated = cached.stream()
                        .filter(json -> {
                            try {
                                ChannelStatusResponse res =
                                        objectMapper.readValue(json, ChannelStatusResponse.class);

                                return !statusId.equals(res.getId());
                            } catch (Exception e) {
                                return true; // keep unknown safely
                            }
                        })
                        .toList();

                redisTemplate.delete(redisKey);

                if (!updated.isEmpty()) {
                    redisTemplate.opsForList().leftPushAll(redisKey, updated);
                    redisTemplate.expire(redisKey, Duration.ofHours(24));
                }
            }

        } catch (Exception e) {
            log.warn("Redis delete cleanup failed", e);
        }

        // =====================================================
        // 🗑 MEDIA DELETE (SAFE)
        // =====================================================
        try {
            if (status.getMedia() != null) {
                for (StatusMedia m : status.getMedia()) {
                    try {
                        mediaService.delete(m.getMediaKey());
                    } catch (Exception ex) {
                        log.warn("Media delete failed: {}", m.getMediaKey());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Media cleanup failed", e);
        }

        // =====================================================
        // 🧹 VIEW CACHE CLEAR
        // =====================================================
        try {
            redisTemplate.delete("status:view:" + statusId);
        } catch (Exception e) {
            log.warn("View cache delete failed", e);
        }

        // =====================================================
        // 🔴 REALTIME EVENT
        // =====================================================
        try {
            wsSendService.sendToChannelSubscribers(
                    channelId,
                    Map.of(
                            "type", "STATUS_DELETE",
                            "statusId", statusId
                    )
            );
        } catch (Exception e) {
            log.warn("WS delete push failed", e);
        }
    }


    private boolean safeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return java.security.MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }

    public boolean isUnseen(String statusId, String userId) {

        if (statusId == null || userId == null) return false;

        try {
            String seenKey = "status:seen:user:" + userId;

            Boolean seen = redisTemplate.opsForSet()
                    .isMember(seenKey, statusId);

            return seen == null || !seen;

        } catch (Exception e) {
            log.warn("isUnseen failed", e);
            return false;
        }
    }
    public Set<String> getSeenStatusIds(String userId) {

        if (userId == null) return Set.of();

        try {
            String key = "status:seen:user:" + userId;

            Set<String> seen = redisTemplate.opsForSet().members(key);

            return seen != null ? seen : Set.of();

        } catch (Exception e) {
            log.warn("getSeenStatusIds failed", e);
            return Set.of();
        }
    }


}