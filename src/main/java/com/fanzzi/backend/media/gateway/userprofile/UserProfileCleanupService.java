package com.fanzzi.backend.media.gateway.userprofile;

import com.fanzzi.backend.media.profile.UserProfileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileCleanupService {

    private final UserProfileStorageService storage;

    @Async
    public void deleteOldProfileImage(String key) {
        try {
            log.info("Deleting old profile image: {}", key);
            storage.deleteUserProfileImage(key);
        } catch (Exception e) {
            log.error("Failed to delete image: {}", key, e);
        }
    }
}