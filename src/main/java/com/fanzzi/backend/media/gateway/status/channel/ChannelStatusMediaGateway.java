package com.fanzzi.backend.media.gateway.status.channel;

public interface ChannelStatusMediaGateway {

    // ===== STATUS URL =====
    String getChannelStatusUrl(String key);

    // ===== PRIVATE DOWNLOAD =====
    String getChannelStatusDownloadUrl(String key);

    // ===== DELETE =====
    void deleteChannelStatusMedia(String key);
}