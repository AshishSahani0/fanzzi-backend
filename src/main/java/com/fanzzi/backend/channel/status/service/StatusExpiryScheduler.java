package com.fanzzi.backend.channel.status.service;

import com.fanzzi.backend.channel.status.repository.ChannelStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusExpiryScheduler {

    private final ChannelStatusRepository repo;

    @Scheduled(fixedRate = 60000) // every 1 min
    public void expireStatuses() {

        try {
            int updated = repo.softExpire(Instant.now());
            if (updated > 0) {
                log.info("Expired {} statuses", updated);
            }
        } catch (Exception e) {
            log.error("Status expiry failed", e);
        }
    }
}