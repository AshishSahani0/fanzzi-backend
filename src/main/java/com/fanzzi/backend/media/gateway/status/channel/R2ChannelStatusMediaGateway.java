package com.fanzzi.backend.media.gateway.status.channel;

import com.fanzzi.backend.media.status.ChannelStatusStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class R2ChannelStatusMediaGateway implements ChannelStatusMediaGateway {

    private final ChannelStatusStorageService statusStorage;

    // ===== PUBLIC URL =====
    @Override
    public String getChannelStatusUrl(String key) {
        return statusStorage.getChannelStatusUrl(key);
    }

    // ===== PRIVATE DOWNLOAD URL =====
    @Override
    public String getChannelStatusDownloadUrl(String key) {
        return statusStorage.getChannelStatusDownloadUrl(key);
    }

    // ===== DELETE =====
    @Override
    public void deleteChannelStatusMedia(String key) {
        statusStorage.deleteChannelStatusMedia(key);
    }
}