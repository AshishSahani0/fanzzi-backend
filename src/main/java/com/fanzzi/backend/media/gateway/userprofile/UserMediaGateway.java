package com.fanzzi.backend.media.gateway.userprofile;

public interface UserMediaGateway {

    // ===== USER PROFILE =====
    String getUserProfileUrl(String key);

    void deleteUserProfileImage(String key);
}