package com.fanzzi.backend.media.status;

import com.fanzzi.backend.media.core.R2CoreStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelStatusStorageService {

    private final R2CoreStorageService core;

    @Value("${r2.status-bucket}")
    private String bucket;

    @Value("${cdn.status-base-url}")
    private String cdn;

    private static final String BASE_PREFIX = "status/channels/";

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4"
    );

    // =====================================================
    // 🚀 HIGH SCALE UPLOAD URL (Partitioned + Secure)
    // =====================================================

    public Map<String, String> createChannelStatusUploadUrl(
            String channelId,
            String fileName,
            long fileSize
    ) {

        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Invalid file name");
        }

        if (fileSize > 20 * 1024 * 1024) {
            throw new RuntimeException("File too large");
        }

        String contentType = resolveContentType(fileName);
        String safeFileName = sanitizeFileName(fileName);

        String hourPartition = Instant.now()
                .truncatedTo(ChronoUnit.HOURS)
                .toString()
                .replace(":", "-");

        String key = BASE_PREFIX
                + channelId + "/"
                + hourPartition + "/"
                + UUID.randomUUID() + "_"
                + safeFileName;

        return core.presignUpload(
                bucket,
                key,
                contentType,
                5,
                fileSize
        );
    }

    private String resolveContentType(String fileName) {
        String lower = fileName.toLowerCase();

        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".aac")) return "audio/aac";

        return "image/jpeg";
    }

    // =====================================================
    // 🌍 PUBLIC CDN URL (For Active Status)
    // =====================================================

    public String getChannelStatusUrl(String key) {
        try {
            validateKey(key);
            return cdn + "/" + key;
        } catch (Exception e) {
            return null; // 🔥 DO NOT CRASH API
        }
    }

    // =====================================================
    // 🔐 PRIVATE SIGNED DOWNLOAD
    // =====================================================
    @Cacheable(
            value = "status_signed_urls",
            key = "#key"
    )
    public String getChannelStatusDownloadUrl(String key) {

        validateKey(key);

        // 2-minute expiry for private access
        return core.presignDownload(bucket, key, 30);
    }

    // =====================================================
    // 🗑 SAFE DELETE
    // =====================================================

    public void deleteChannelStatusMedia(String key) {

        validateKey(key);

        core.delete(bucket, key);
    }

    // =====================================================
    // 🔒 VALIDATION HELPERS
    // =====================================================

    private void validateContentType(String contentType) {
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Unsupported file type");
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new RuntimeException("Invalid key");
        }

        /// ✅ ALLOW STATUS MEDIA
        if (key.startsWith(BASE_PREFIX)) {
            return;
        }

        /// ✅ ALLOW PROFILE IMAGES (🔥 CRITICAL FIX)
        if (key.startsWith("profiles/")) {
            return;
        }

        /// ❌ BLOCK EVERYTHING ELSE
        throw new RuntimeException("Invalid media key: " + key);
    }

    private String sanitizeFileName(String fileName) {
        return fileName
                .replaceAll("[^a-zA-Z0-9\\.\\-_]", "_")
                .toLowerCase();
    }
}