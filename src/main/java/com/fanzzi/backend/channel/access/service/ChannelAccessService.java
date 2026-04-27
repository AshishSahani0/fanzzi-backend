package com.fanzzi.backend.channel.access.service;

import com.fanzzi.backend.channel.access.dto.ChannelAccess;
import com.fanzzi.backend.channel.block.repository.ChannelBlockRepository;
import com.fanzzi.backend.channel.enums.ChannelType;
import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.subscription.model.ChannelSubscription;
import com.fanzzi.backend.channel.subscription.repository.ChannelSubscriptionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChannelAccessService {

    private final ChannelRepository channelRepo;
    private final ChannelMemberRepository memberRepo;
    private final ChannelSubscriptionRepository subRepo;

    // 🔥 ADD THIS
    private final ChannelBlockRepository blockRepo;

    public ChannelAccess resolve(String channelId, String userId) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        // =====================================================
        // 🚫 BLOCK CHECK (HIGHEST PRIORITY)
        // =====================================================
        if (blockRepo.existsByChannelIdAndUserId(channelId, userId)) {
            return ChannelAccess.noAccess(); // 🔥 HARD STOP
        }

        // =====================================================
        // 👑 OWNER → FULL ACCESS
        // =====================================================
        boolean isOwner = channel.getOwnerId().equals(userId);

        if (isOwner) {
            return ChannelAccess.subscriberAccess();
        }

        // =====================================================
        // 👥 ACTIVE MEMBERSHIP ONLY
        // =====================================================
        boolean isMember =
                memberRepo.existsByChannelIdAndUserIdAndLeftFalse(
                        channelId,
                        userId
                );

        if (!isMember) {
            return ChannelAccess.noAccess();
        }

        // =====================================================
        // 🆓 FREE CHANNEL
        // =====================================================
        if (channel.getType() == ChannelType.FREE) {
            return ChannelAccess.memberAccess();
        }

        // =====================================================
        // 💰 PAID CHANNEL
        // =====================================================
        ChannelSubscription subscription =
                subRepo.findByUserIdAndChannelId(userId, channelId)
                        .orElse(null);

        if (subscription == null || !subscription.isActive()) {
            return ChannelAccess.expiredAccess();
        }

        if (subscription.getExpiresAt() != null &&
                subscription.getExpiresAt().isBefore(Instant.now())) {

            return ChannelAccess.expiredAccess();
        }

        // =====================================================
        // ✅ VALID SUBSCRIPTION
        // =====================================================
        return ChannelAccess.subscriberAccess();
    }
}