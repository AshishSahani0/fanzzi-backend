package com.fanzzi.backend.media.profile;

import com.fanzzi.backend.media.core.R2CoreStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileStorageService {

    private final R2CoreStorageService core;

    @Value("${r2.profiles-bucket}")
    private String bucket;

    @Value("${cdn.profile-base-url}")
    private String cdn;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // ===== UPLOAD =====
    public Map<String, String> createUserProfileUploadUrl(
            String userId,
            String fileName,
            long fileSize
    ) {

        // 🔥 SIZE LIMIT
        if (fileSize > 5 * 1024 * 1024) {
            throw new RuntimeException("Profile image too large");
        }

        String contentType = resolveContentType(fileName);
        String extension = getExtension(contentType);

        String key = "profiles/users/"
                + userId + "/"
                + UUID.randomUUID() + extension;

        return core.presignUpload(
                bucket,
                key,
                contentType,
                10,
                fileSize // 🔥 REQUIRED
        );
    }
    private String resolveContentType(String fileName) {
        String lower = fileName.toLowerCase();

        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";

        return "image/jpeg";
    }
    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/jpeg" -> ".jpg";
            default -> ".jpg";
        };
    }

    // ===== GET PUBLIC CDN URL =====
    public String getUserProfileUrl(String key) {
        String url = key == null ? null : cdn + "/" + key;
        System.out.println("PROFILE IMAGE URL: " + url);
        return url;

    }

    // ===== DELETE =====
    public void deleteUserProfileImage(String key) {
        if (key == null || key.isBlank()) return;
        core.delete(bucket, key);
    }
}