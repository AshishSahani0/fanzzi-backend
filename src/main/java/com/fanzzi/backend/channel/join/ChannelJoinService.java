package com.fanzzi.backend.channel.join;

import com.fanzzi.backend.channel.block.repository.ChannelBlockRepository;
import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.channel.enums.ChannelRole;
import com.fanzzi.backend.channel.enums.ChannelVisibility;
import com.fanzzi.backend.channel.event.ChannelEvent;
import com.fanzzi.backend.channel.event.ChannelEventPublisher;
import com.fanzzi.backend.channel.event.ChannelEventType;
import com.fanzzi.backend.channel.membership.model.ChannelMember;
import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChannelJoinService {

    private final ChannelRepository channelRepo;
    private final ChannelMemberRepository memberRepo;
    private final ChannelBlockRepository blockRepo;
    private final ChannelEventPublisher eventPublisher;



    public ChannelResponse joinBySlug(String slug, String userId) {

        Channel channel = channelRepo.findBySlug(slug)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        if (channel.getVisibility() != ChannelVisibility.PUBLIC) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Channel is not public"
            );
        }

        return joinChannel(channel, userId);
    }



    public ChannelResponse joinByInviteToken(String token, String userId) {

        Channel channel = channelRepo.findByInviteToken(token)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Invalid invite link"
                        )
                );

        return joinChannel(channel, userId);
    }



    public ChannelResponse joinChannel(Channel channel, String userId) {

        String channelId = channel.getId();


        if (channel.getModerationStatus() != ChannelModerationStatus.NORMAL) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Channel not available"
            );
        }

        // =====================================================
        // 🚫 BLOCK CHECK (HARD STOP)
        // =====================================================
        if (blockRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You have blocked this channel. Unblock to join."
            );
        }

        Instant now = Instant.now();

        // =====================================================
        // 🔁 MEMBERSHIP REUSE (TELEGRAM STYLE)
        // =====================================================
        ChannelMember existing = memberRepo
                .findByChannelIdAndUserId(channelId, userId)
                .orElse(null);

        long updatedCount;

        if (existing != null) {

            if (!existing.isLeft()) {
                return buildChannelResponse(
                        channel,
                        userId,
                        channel.getMemberCount()
                );
            }

            if (existing.getLeftAt() != null &&
                    existing.getLeftAt().isAfter(now.minusSeconds(10))) {

                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait before rejoining"
                );
            }

            existing.setLeft(false);
            existing.setLeftAt(null);
            existing.setJoinedAt(now);

            memberRepo.save(existing);

            updatedCount = channelRepo.incrementMemberCountAndGet(channelId);

        } else {

            try {
                memberRepo.insert(
                        ChannelMember.builder()
                                .channelId(channelId)
                                .userId(userId)
                                .joinedAt(now)
                                .left(false)
                                .role(ChannelRole.MEMBER)
                                .build()
                );

                updatedCount = channelRepo.incrementMemberCountAndGet(channelId);

            } catch (DuplicateKeyException ignored) {
                updatedCount = channelRepo.incrementMemberCountAndGet(channelId);
            }
        }




        eventPublisher.publish(
                new ChannelEvent(
                        channelId,
                        userId,
                        ChannelEventType.JOIN,
                        java.util.Map.of(
                                "channelId", channelId,
                                "userId", userId,
                                "joinedAt", now,
                                "memberCount", updatedCount
                        )
                )
        );

// =====================================================
// 🔄 RETURN RESPONSE
// =====================================================
        return buildChannelResponse(channel, userId, updatedCount);
    }

    /* ===================================================== */
    /* 🔥 RESPONSE BUILDER (CRITICAL FIXED) */
    /* ===================================================== */

    private ChannelResponse buildChannelResponse(
            Channel channel,
            String userId,
            long memberCount
    ) {

        boolean isOwner = channel.getOwnerId().equals(userId);

        boolean isMember = true; // already joined
        boolean blocked = false;

        return ChannelResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .profileImageKey(channel.getProfileImageKey())

                .visibility(channel.getVisibility())
                .type(channel.getType())
                .monthlyPrice(channel.getMonthlyPrice())

                .slug(channel.getSlug())
                .inviteToken(channel.getInviteToken())

                .memberCount(memberCount) // ✅ FIXED
                .subscriberCount(channel.getSubscriberCount())
                .postCount(channel.getPostCount())

                .owner(isOwner)
                .member(isMember)
                .joined(true)

                .subscribed(false)

                .canRead(true)
                .canPost(true)
                .blurred(false)

                .hasActiveStatus(false)
                .blocked(false)

                .build();
    }
    /* ===================================================== */
    /* JOIN BY CHANNEL ID */
    /* ===================================================== */

    public ChannelResponse joinByChannelId(String channelId, String userId) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        return joinChannel(channel, userId);
    }
}