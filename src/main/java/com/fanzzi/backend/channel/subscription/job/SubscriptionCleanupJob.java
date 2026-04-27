package com.fanzzi.backend.channel.subscription.job;

import com.fanzzi.backend.channel.subscription.repository.ChannelSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubscriptionCleanupJob {

    private final ChannelSubscriptionRepository repository;

    // Every 15 minutes (safe balance between load & freshness)
    @Scheduled(cron = "0 */15 * * * *")
    public void deactivateExpiredSubscriptions() {

        Instant now = Instant.now();

        long affected = repository.deactivateExpired(now);

        if (affected > 0) {
            System.out.println("Deactivated expired subscriptions: " + affected);
        }
    }
}