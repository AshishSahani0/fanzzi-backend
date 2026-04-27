package com.fanzzi.backend.wallets.stars.monetization;

import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channel")
@RequiredArgsConstructor
public class ChannelSubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    public void subscribe(
            @RequestParam String channelId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        subscriptionService.subscribe(
                userId,
                channelId
        );
    }
}