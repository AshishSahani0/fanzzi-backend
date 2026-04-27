package com.fanzzi.backend.channel.leave;
import com.fanzzi.backend.channel.subscription.repository.ChannelSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelCleanupListener {

    private final ChannelSubscriptionRepository subRepo;

    @EventListener
    public void handle(ChannelCleanupEvent event) {

        String channelId = event.getChannelId();
        String userId = event.getUserId();

        subRepo.deleteByChannelIdAndUserId(channelId, userId);

        log.info("🧹 Subscription cleaned → user={}, channel={}", userId, channelId);
    }
}