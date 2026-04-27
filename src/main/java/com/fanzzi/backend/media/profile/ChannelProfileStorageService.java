package com.fanzzi.backend.media.profile;

import com.fanzzi.backend.media.core.R2CoreStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelProfileStorageService {

    private final R2CoreStorageService core;

    @Value("${r2.profiles-bucket}")
    private String bucket;

    @Value("${cdn.profile-base-url}")
    private String cdn;

    private static final String BASE_PREFIX = "channel-profiles/";

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // =====================================================
    // 🚀 PRESIGNED UPLOAD URL (Partitioned + Secure)
    // =====================================================

    public Map<String, String> createChannelProfileUploadUrl(
            String fileName,
            long fileSize
    ) {

        // 🔥 SIZE LIMIT
        if (fileSize > 5 * 1024 * 1024) {
            throw new RuntimeException("Profile image too large");
        }

        // 🔥 SAFE TYPE
        String contentType = resolveContentType(fileName);
        String extension = resolveExtension(contentType);

        String hourPartition = Instant.now()
                .truncatedTo(ChronoUnit.HOURS)
                .toString()
                .replace(":", "-");

        String key = BASE_PREFIX
                + hourPartition + "/"
                + UUID.randomUUID()
                + extension;

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

    // =====================================================
    // 🌍 PUBLIC CDN URL
    // =====================================================

    public String getChannelProfileUrl(String key) {

        validateKey(key);

        return cdn + "/" + key;
    }

    // =====================================================
    // 🗑 SAFE DELETE
    // =====================================================

    public void deleteChannelProfileImage(String key) {

        validateKey(key);

        core.delete(bucket, key);
    }

    // =====================================================
    // 🔒 VALIDATION
    // =====================================================

    private void validateContentType(String contentType) {
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Unsupported file type");
        }
    }

    private void validateKey(String key) {
        if (key == null || !key.startsWith(BASE_PREFIX)) {
            throw new RuntimeException("Invalid profile key");
        }
    }

    private String resolveExtension(String contentType) {

        if (contentType == null) return ".jpg";

        if (contentType.contains("png")) return ".png";
        if (contentType.contains("webp")) return ".webp";
        if (contentType.contains("jpeg")) return ".jpg";

        return ".jpg";
    }
}