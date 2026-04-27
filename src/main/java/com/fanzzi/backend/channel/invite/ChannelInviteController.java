package com.fanzzi.backend.channel.invite;

import com.fanzzi.backend.channel.invite.dto.SendChannelInviteRequest;
import com.fanzzi.backend.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/invite")
@RequiredArgsConstructor
public class ChannelInviteController {

    private final ChannelInviteService inviteService;

    @PostMapping("/send")
    public void sendInvite(@Valid @RequestBody SendChannelInviteRequest request) {

        String userId = SecurityUtil.getCurrentUserId();

        inviteService.sendInvite(
                userId,
                request.getTargetChannelId(),
                request.getInviteChannelId()
        );
    }
}