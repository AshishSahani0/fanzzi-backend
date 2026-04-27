package com.fanzzi.backend.media.controller;

import com.fanzzi.backend.media.profile.UserProfileStorageService;
import com.fanzzi.backend.media.profile.ChannelProfileStorageService;
import com.fanzzi.backend.media.post.PostMediaStorageService;
import com.fanzzi.backend.media.status.ChannelStatusStorageService;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final UserProfileStorageService userProfileStorage;
    private final ChannelProfileStorageService channelProfileStorage;
    private final PostMediaStorageService postMediaStorage;
    private final ChannelStatusStorageService statusStorage;

    // =====================================================
    // 🟢 USER PROFILE IMAGE (PUBLIC)
    // =====================================================

    @PostMapping("/profile/upload-url")
    public Map<String, String> userProfileUploadUrl(
            @RequestParam String fileName,
            @RequestParam long fileSize
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        return userProfileStorage.createUserProfileUploadUrl(
                userId,
                fileName,
                fileSize
        );
    }

    // =====================================================
    // 📣 CHANNEL PROFILE IMAGE (PUBLIC)
    // =====================================================

    @PostMapping("/channel-profile/upload-url")
    public Map<String, String> channelProfileUploadUrl(
            @RequestParam String fileName,
            @RequestParam long fileSize
    ) {
        return channelProfileStorage.createChannelProfileUploadUrl(
                fileName,
                fileSize
        );
    }

    // =====================================================
    // 🔴 POST MEDIA (PRIVATE)
    // =====================================================

    @PostMapping("/post/upload-url")
    public Map<String, String> postUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam long fileSize
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        return postMediaStorage.createPostMediaUploadUrl(
                userId,
                fileName,
                contentType,
                fileSize
        );
    }

    @DeleteMapping("/status/media")
    public void deleteStatusMedia(@RequestParam String key) {
        statusStorage.deleteChannelStatusMedia(key);
    }


    // =====================================================
    // 🔒 CHANNEL STATUS MEDIA (PRIVATE)
    // =====================================================

//    @PostMapping("/status/upload-url")
//    public Map<String, String> statusUploadUrl(
//            @RequestParam String channelId,
//            @RequestParam String fileName,
//            @RequestParam String contentType
//    ) {
//
//        // 🔒 TODO: verify channel membership / ownership
//
//        return statusStorage.createChannelStatusUploadUrl(
//                channelId,
//                fileName,
//                contentType
//        );
//    }
}