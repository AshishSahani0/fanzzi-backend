package com.fanzzi.backend.channel.create;

import com.fanzzi.backend.channel.common.ChannelMapper;
import com.fanzzi.backend.channel.dto.request.CreateChannelRequest;
import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.channel.enums.*;
import com.fanzzi.backend.channel.membership.model.ChannelMember;
import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;
import com.fanzzi.backend.channel.stats.ChannelStats;
import com.fanzzi.backend.channel.stats.ChannelStatsRepository;
import com.fanzzi.backend.channel.util.ChannelSlugUtil;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChannelCreateService {

    private final ChannelRepository channelRepository;
    private final ChannelStatsRepository statsRepository;
    private final ChannelMemberRepository memberRepository;
    private final ChannelMapper mapper;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_RETRIES = 5;
    private static final int TOKEN_LENGTH = 12;
    private final SimpMessagingTemplate messagingTemplate;
    private static final long MAX_MONTHLY_PRICE = 1000;
    private static final long MIN_MONTHLY_PRICE = 1;

    @CacheEvict(
            value = {
                    "my_channels",
                    "joined_channels",
                    "archived_channels"
            },
            key = "'user:' + #ownerId",
            allEntries = true
    )
    public ChannelResponse createChannel(String ownerId,
                                         CreateChannelRequest request) {

        validatePaidConfiguration(request);

        String validatedName = validateAndNormalizeName(request.getName());

        Channel channel = buildChannel(ownerId, validatedName, request);

        Channel savedChannel = persistWithRetry(channel);

        initializeStats(savedChannel.getId());
        ensureOwnerMembership(savedChannel.getId(), ownerId);

        ChannelResponse response = mapper.toResponse(savedChannel, ownerId);


        publishChannelCreatedEvent(response);

        return response;
    }

    private void publishChannelCreatedEvent(ChannelResponse channel) {
        try {
            Map<String, Object> event = Map.of(
                    "type", "CHANNEL_CREATE",
                    "payload", channel
            );

            messagingTemplate.convertAndSend("/topic/chat", event);

        } catch (Exception ignored) {}
    }

    /* ============================================================ */
    /* ===================== CORE PERSIST ========================= */
    /* ============================================================ */

    private Channel persistWithRetry(Channel channel) {

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {

            assignAccessIdentifier(channel);

            try {
                return channelRepository.insert(channel); // strict create
            } catch (DuplicateKeyException ignored) {
                // regenerate slug/token and retry
            }
        }

        throw new ApiException(
                ErrorCode.CONFLICT,
                "Failed to generate unique channel identifier"
        );
    }

    /* ============================================================ */
    /* ===================== STATS INIT =========================== */
    /* ============================================================ */

    private void initializeStats(String channelId) {

        try {
            statsRepository.insert(
                    ChannelStats.builder()
                            .channelId(channelId)
                            .memberCount(0)
                            .subscriberCount(0)
                            .postCount(0)
                            .viewCount(0)
                            .reactionCount(0)
                            .shareCount(0)
                            .updatedAt(Instant.now())
                            .build()
            );
        } catch (DuplicateKeyException ignored) {
            // idempotent safety
        }
    }

    /* ============================================================ */
    /* ===================== OWNER MEMBERSHIP ===================== */
    /* ============================================================ */

    private void ensureOwnerMembership(String channelId, String ownerId) {

        try {
            memberRepository.insert(
                    ChannelMember.builder()
                            .channelId(channelId)
                            .userId(ownerId)
                            .role(ChannelRole.OWNER)
                            .joinedAt(Instant.now())
                            .left(false)
                            .build()
            );
        } catch (DuplicateKeyException ignored) {
            // idempotent safety
        }
    }

    /* ============================================================ */
    /* ===================== IDENTIFIER =========================== */
    /* ============================================================ */

    private void assignAccessIdentifier(Channel channel) {

        if (channel.getVisibility() == ChannelVisibility.PUBLIC) {
            channel.setSlug(generateSlug(channel.getName()));
        } else {
            channel.setInviteToken(generateToken(TOKEN_LENGTH));
        }
    }

    private String generateSlug(String name) {

        String base = ChannelSlugUtil.toSlugBase(name);

        int randomSuffix = 1000 + RANDOM.nextInt(9000);

        return base + "-" + randomSuffix;
    }

    private String generateToken(int length) {

        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes)
                .substring(0, length);
    }

    /* ============================================================ */
    /* ===================== VALIDATION =========================== */
    /* ============================================================ */

    private String validateAndNormalizeName(String rawName) {

        if (rawName == null || rawName.isBlank()) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Channel name is required"
            );
        }

        String name = rawName.trim();

        if (name.length() > 80) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Channel name too long"
            );
        }

        if (!name.matches("^[a-zA-Z0-9 _-]+$")) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Channel name contains invalid characters"
            );
        }

        return name;
    }

    private void validatePaidConfiguration(CreateChannelRequest request) {

        if (request.getType() == ChannelType.PAID) {

            Long price = request.getMonthlyPrice();

            // null safety
            if (price == null) {
                throw new ApiException(
                        ErrorCode.INVALID_REQUEST,
                        "Monthly price is required"
                );
            }

            // range validation
            if (price < MIN_MONTHLY_PRICE || price > MAX_MONTHLY_PRICE) {
                throw new ApiException(
                        ErrorCode.INVALID_REQUEST,
                        "Price must be between ₹1 and ₹1000"
                );
            }

        } else {
            request.setMonthlyPrice(null);
        }
    }

    /* ============================================================ */
    /* ===================== BUILDER ============================== */
    /* ============================================================ */

    private Channel buildChannel(String ownerId,
                                 String name,
                                 CreateChannelRequest request) {

        boolean discoverable =
                request.getVisibility() == ChannelVisibility.PUBLIC
                        && Boolean.TRUE.equals(request.getDiscoverable());

        Instant now = Instant.now();

        return Channel.builder()
                .ownerId(ownerId)
                .name(name)
                .nameLower(name.toLowerCase())
                .description(
                        request.getDescription() == null
                                ? null
                                : request.getDescription().trim()
                )
                .profileImageKey(request.getProfileImageKey())
                .visibility(request.getVisibility())
                .type(request.getType())
                .monthlyPrice(request.getMonthlyPrice())
                .category(request.getCategory())
                .language(request.getLanguage())
                .discoverable(discoverable)
                .moderationStatus(ChannelModerationStatus.NORMAL)
                .createdAt(now)
                .updatedAt(now)
                .memberCount(1) // owner
                .build();
    }
}