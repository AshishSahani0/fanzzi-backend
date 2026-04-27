package com.fanzzi.backend.channel.invite;

import com.fanzzi.backend.channel.enums.ChannelVisibility;
import com.fanzzi.backend.channel.message.service.ChannelMessageService;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.util.ChannelInviteUtil;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChannelInviteService {

    private final ChannelRepository channelRepository;
    private final ChannelMessageService messageService;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendInvite(
            String senderId,
            String targetChannelId,
            String inviteChannelId
    ) {

        if (targetChannelId.equals(inviteChannelId)) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Cannot share a channel inside itself"
            );
        }


        Channel target = channelRepository.findById(targetChannelId)
                .orElseThrow(() ->
                        new ApiException(
                                ErrorCode.NOT_FOUND,
                                "Target channel not found"
                        ));

        if (target.isDeleted()) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Target channel is deleted"
            );
        }

        // Must be owner of target channel
        if (!target.getOwnerId().equals(senderId)) {
            throw new ApiException(
                    ErrorCode.FORBIDDEN,
                    "You can only share inside channels you own"
            );
        }



        Channel inviteChannel = channelRepository.findById(inviteChannelId)
                .orElseThrow(() ->
                        new ApiException(
                                ErrorCode.NOT_FOUND,
                                "Channel to share not found"
                        ));

        if (inviteChannel.isDeleted()) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Cannot share deleted channel"
            );
        }

        if (inviteChannel.getModerationStatus() != ChannelModerationStatus.NORMAL) {
            throw new ApiException(
                    ErrorCode.FORBIDDEN,
                    "Channel is not available for sharing"
            );
        }


        String inviteLink = ChannelInviteUtil.buildInviteLink(
                baseUrl,
                inviteChannel.getSlug(),
                inviteChannel.getInviteToken(),
                inviteChannel.getVisibility() == ChannelVisibility.PUBLIC
        );



        Map<String, Object> payload = Map.of(
                "type", "CHANNEL_INVITE",
                "channelId", inviteChannel.getId(),
                "name", inviteChannel.getName(),
                "profileImageKey", inviteChannel.getProfileImageKey(),
                "inviteLink", inviteLink
        );



        messageService.sendSystemMessage(
                targetChannelId,
                payload
        );
    }
}