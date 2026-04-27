package com.fanzzi.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {

    public void logChannelUpdate(String channelId, String userId) {
        log.info("CHANNEL_UPDATED | channelId={} | userId={}", channelId, userId);
    }
}