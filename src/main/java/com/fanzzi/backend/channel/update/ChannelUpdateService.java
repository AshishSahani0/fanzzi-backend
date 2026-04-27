package com.fanzzi.backend.channel.update;

import com.fanzzi.backend.channel.dto.request.UpdateChannelRequest;
import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.channel.enums.ChannelType;
import com.fanzzi.backend.channel.enums.ChannelVisibility;
import com.fanzzi.backend.channel.event.ChannelEvent;
import com.fanzzi.backend.channel.event.ChannelEventPublisher;
import com.fanzzi.backend.channel.event.ChannelEventType;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.common.ChannelMapper;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.config.AsyncExecutor;
import com.fanzzi.backend.config.AuditService;
import com.fanzzi.backend.common.infrastructure.rate_limit.RateLimitService;
import com.fanzzi.backend.media.gateway.channelprofile.ChannelMediaGateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;


@Service
@RequiredArgsConstructor
public class ChannelUpdateService {

    private final ChannelRepository repo;
    private final ChannelMediaGateway media;
    private final ChannelMapper mapper;
    private final RateLimitService rateLimitService;
    private final AsyncExecutor asyncExecutor;
    private final AuditService audit;
    private final ChannelEventPublisher eventPublisher;

    @Transactional
    public ChannelResponse updateChannel(
            String channelId,
            String userId,
            UpdateChannelRequest req,
            String idemKey
    ) {

        Channel ch = repo.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Channel not found"));

        // 🔐 SECURITY
        if (!ch.getOwnerId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only owner allowed");
        }

        if (ch.isDeleted()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Channel deleted");
        }

        if (ch.getModerationStatus() != ChannelModerationStatus.NORMAL) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Channel locked");
        }

        // ⚡ RATE LIMIT
        rateLimitService.checkLimit(
                "rate:update:channel:" + userId,
                10,
                Duration.ofMinutes(1)
        );

        boolean updated = false;
        String oldImageKey = ch.getProfileImageKey();

        // ================= UPDATE LOGIC =================

        if (req.getProfileImageKey() != null &&
                !req.getProfileImageKey().equals(oldImageKey)) {
            ch.setProfileImageKey(req.getProfileImageKey());
            updated = true;
        }

        if (req.getName() != null) {
            String name = sanitize(req.getName());

            if (name.length() < 3 || name.length() > 100) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid name");
            }

            if (!name.equals(ch.getName())) {
                ch.setName(name);
                ch.setNameLower(name.toLowerCase());
                updated = true;
            }
        }

        if (req.getDescription() != null) {
            String desc = sanitize(req.getDescription());

            if (!desc.equals(ch.getDescription())) {
                ch.setDescription(desc);
                updated = true;
            }
        }

        if (req.getVisibility() != null &&
                req.getVisibility() != ch.getVisibility()) {
            ch.setVisibility(req.getVisibility());
            updated = true;
        }

        if (req.getType() != null) {

            if (req.getType() == ChannelType.PAID) {

                Long price = req.getMonthlyPrice();

                if (price == null || price <= 0) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid price");
                }

                if (ch.getType() != ChannelType.PAID ||
                        !price.equals(ch.getMonthlyPrice())) {

                    ch.setType(ChannelType.PAID);
                    ch.setMonthlyPrice(price);
                    updated = true;
                }

            } else {
                if (ch.getType() != ChannelType.FREE) {
                    ch.setType(ChannelType.FREE);
                    ch.setMonthlyPrice(null);
                    updated = true;
                }
            }
        }

        if (req.getCategory() != null &&
                !req.getCategory().equals(ch.getCategory())) {
            ch.setCategory(req.getCategory());
            updated = true;
        }

        if (req.getLanguage() != null &&
                !req.getLanguage().equals(ch.getLanguage())) {
            ch.setLanguage(req.getLanguage());
            updated = true;
        }

        if (req.getDiscoverable() != null &&
                req.getDiscoverable() != ch.isDiscoverable()) {
            ch.setDiscoverable(req.getDiscoverable());
            updated = true;
        }

        if (req.getNsfw() != null &&
                req.getNsfw() != ch.isNsfw()) {
            ch.setNsfw(req.getNsfw());
            updated = true;
        }

        // 🔒 CONSISTENCY
        if (ch.getVisibility() == ChannelVisibility.PRIVATE) {
            if (ch.isDiscoverable()) {
                ch.setDiscoverable(false);
                updated = true;
            }
        }

        // =====================================================
        // 💾 SAVE + REALTIME
        // =====================================================

        if (updated) {

            ch.setUpdatedAt(Instant.now());
            repo.save(ch);

            ChannelResponse response = mapper.toResponse(ch, userId);

            // =====================================================
            // 🔥 EVENT (CLEAN ARCHITECTURE)
            // =====================================================
            eventPublisher.publish(
                    new ChannelEvent(
                            ch.getId(),
                            userId,
                            ChannelEventType.UPDATE,
                            response
                    )
            );

            // =====================================================
            // 🧹 CLEANUP (ASYNC)
            // =====================================================
            if (oldImageKey != null &&
                    req.getProfileImageKey() != null &&
                    !oldImageKey.equals(req.getProfileImageKey())) {

                asyncExecutor.run(() ->
                        media.deleteChannelProfileImage(oldImageKey)
                );
            }

            audit.logChannelUpdate(ch.getId(), userId);

            return response;
        }

        // 👉 no changes
        return mapper.toResponse(ch, userId);
    }

    private String sanitize(String input) {
        return input == null ? null : input.trim().replaceAll("\\s+", " ");
    }
}