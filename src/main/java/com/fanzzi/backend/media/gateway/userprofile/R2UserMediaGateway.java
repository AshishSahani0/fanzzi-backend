package com.fanzzi.backend.media.gateway.userprofile;

import com.fanzzi.backend.media.profile.UserProfileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class R2UserMediaGateway implements UserMediaGateway {

    private final UserProfileStorageService userProfileStorage;

    // ===== GET USER PROFILE URL =====
    @Override
    public String getUserProfileUrl(String key) {
        return userProfileStorage.getUserProfileUrl(key);
    }

    // ===== DELETE USER PROFILE IMAGE =====
    @Override
    public void deleteUserProfileImage(String key) {
        userProfileStorage.deleteUserProfileImage(key);
    }
}