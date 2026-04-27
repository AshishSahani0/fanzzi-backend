package com.fanzzi.backend.channel.invite.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendChannelInviteRequest {

    @NotBlank
    private String targetChannelId; // where message will be posted

    @NotBlank
    private String inviteChannelId; // channel being shared
}
