package com.fanzzi.backend.media.gateway.channelprofile;

import com.fanzzi.backend.media.profile.ChannelProfileStorageService;
import com.fanzzi.backend.media.profile.UserProfileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelR2MediaGateway implements ChannelMediaGateway {

    private final ChannelProfileStorageService channelProfileStorage;
    private final UserProfileStorageService userProfileStorage;

    // ===== CHANNEL PROFILE URL =====
    @Override
    public String getChannelProfileUrl(String key) {
        return channelProfileStorage.getChannelProfileUrl(key);
    }

    // ===== DELETE CHANNEL PROFILE =====
    @Override
    public void deleteChannelProfileImage(String key) {
        channelProfileStorage.deleteChannelProfileImage(key);
    }


}