package com.fanzzi.backend.channel.common;

import com.fanzzi.backend.channel.block.repository.ChannelBlockRepository;
import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.access.service.ChannelAccessService;
import com.fanzzi.backend.channel.status.repository.ChannelStatusRepository;
import com.fanzzi.backend.channel.util.ChannelInviteUtil;
import com.fanzzi.backend.media.gateway.channelprofile.ChannelMediaGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ChannelMapper {

    private final ChannelMediaGateway media;
    private final ChannelAccessService accessService;
    private final ChannelStatusRepository statusRepo;
    private final ChannelBlockRepository blockRepo;

    @Value("${app.base-url}")
    private String baseUrl;

    public ChannelResponse toResponse(Channel ch, String userId) {

        String profileUrl = ch.getProfileImageKey() != null
                ? media.getChannelProfileUrl(ch.getProfileImageKey())
                : null;

        // =====================================================
        // 🚫 BLOCK CHECK FIRST (CRITICAL)
        // =====================================================
        boolean blocked = false;

        if (userId != null) {
            blocked = blockRepo.existsByChannelIdAndUserId(
                    ch.getId(),
                    userId
            );
        }

        // =====================================================
        // 👑 OWNER
        // =====================================================
        boolean isOwner = ch.getOwnerId().equals(userId);

        // =====================================================
        // 🔥 ACCESS (SKIP IF BLOCKED)
        // =====================================================
        var access = blocked
                ? null
                : accessService.resolve(ch.getId(), userId);

        boolean isMember = false;
        boolean isJoined = false;

        if (!blocked && access != null) {
            isMember = access.member();

            if (isOwner) {
                isMember = true;
            }

            isJoined = isMember ;
        }

        // =====================================================
        // 📊 STATUS
        // =====================================================
        boolean hasStatus =
                statusRepo.existsByChannelIdAndDeletedFalseAndExpiresAtAfter(
                        ch.getId(),
                        Instant.now()
                );

        String inviteLink = ChannelInviteUtil.buildInviteLink(
                baseUrl,
                ch.getSlug(),
                ch.getInviteToken(),
                ch.getVisibility() != null &&
                        ch.getVisibility().name().equals("PUBLIC")
        );

        // =====================================================
        // 🚀 FINAL RESPONSE
        // =====================================================
        return ChannelResponse.builder()
                .id(ch.getId())
                .name(ch.getName())
                .description(ch.getDescription())

                // COUNTERS
                .memberCount(ch.getMemberCount())
                .subscriberCount(ch.getSubscriberCount())
                .postCount(ch.getPostCount())

                // BASIC
                .visibility(ch.getVisibility())
                .type(ch.getType())
                .monthlyPrice(ch.getMonthlyPrice())
                .profileImageUrl(profileUrl)
                .inviteLink(inviteLink)

                // FLAGS
                .owner(isOwner)
                .member(isMember)
                .joined(isJoined)
                .subscribed(!blocked && access != null && access.subscribed())

                // 🔥 BLOCK OVERRIDES EVERYTHING
                .canRead(!blocked && access != null && access.canRead())
                .blurred(blocked || (access != null && access.blurred()))
                .canPost(!blocked && access != null && access.canPost())

                .hasActiveStatus(hasStatus)

                // META
                .category(ch.getCategory())
                .language(ch.getLanguage())
                .discoverable(ch.isDiscoverable())
                .nsfw(ch.isNsfw())

                // 🔥 MOST IMPORTANT FLAG
                .blocked(blocked)

                .build();
    }
}