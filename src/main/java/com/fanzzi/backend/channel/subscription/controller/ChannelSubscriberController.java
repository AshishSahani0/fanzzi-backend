package com.fanzzi.backend.channel.subscription.controller;

import com.fanzzi.backend.channel.subscription.service.ChannelSubscriberService;
import com.fanzzi.backend.common.dto.PagedResponse;
import com.fanzzi.backend.security.SecurityUtil;
import com.fanzzi.backend.wallets.stars.monetization.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/channels/{channelId}/subscribers")
@RequiredArgsConstructor
public class ChannelSubscriberController {

    private final ChannelSubscriberService service;
    private final SubscriptionService  subscriptionService;


    @GetMapping
    public PagedResponse<?> getSubscribers(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.getSubscribers(channelId, page, size);
    }

    @GetMapping("/count")
    public Map<String, Long> getSubscriberCount(
            @PathVariable String channelId
    ) {
        long count = service.getSubscriberCount(channelId);
        return Map.of("count", count);
    }
}