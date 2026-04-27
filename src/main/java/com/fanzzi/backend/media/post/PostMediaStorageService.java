package com.fanzzi.backend.media.post;

import com.fanzzi.backend.media.core.R2CoreStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostMediaStorageService {

    private final R2CoreStorageService core;

    @Value("${r2.media-bucket}")
    private String bucket;

    @Value("${cdn.media-base-url}")
    private String cdnBaseUrl;

    private static final String BASE_PREFIX = "media/posts/";

    private static final long MAX_UPLOAD_SIZE = 500L * 1024 * 1024; // 500MB

    private final R2CoreStorageService r2CoreStorageService;

    private static final Set<String> ALLOWED_TYPES = Set.of(

            // images
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",

            // video
            "video/mp4",
            "video/webm",

            // audio
            "audio/mpeg",
            "audio/mp3",
            "audio/aac",

            // files
            "application/pdf",
            "application/zip"
    );

    // ==========================================================
    // 🚀 GENERATE UPLOAD URL
    // ==========================================================

    public Map<String, String> createPostMediaUploadUrl(
            String userId,
            String fileName,
            String contentType,
            long fileSize
    ) {

        validateContentType(contentType);
        validateFileSize(fileSize);
        validateFileName(fileName);

        String safeFileName = sanitizeFileName(fileName);

        String key = BASE_PREFIX
                + userId + "/"
                + Instant.now().getEpochSecond() + "/"
                + UUID.randomUUID() + "_" + safeFileName;

        return core.presignUpload(
                bucket,
                key,
                contentType,
                15,
                fileSize // 🔥 FIXED
        );
    }


    // ==========================================================
    // 📥 CDN DOWNLOAD URL
    // ==========================================================

    public String getPostMediaDownloadUrl(String key) {

        if (key == null || key.isBlank()) {
            return null;
        }

        validateKey(key);

        return cdnBaseUrl + "/" + key;
    }

    public String getSignedDownloadUrl(String key, Duration expiry) {

        int minutes = (int) expiry.toMinutes();

        return r2CoreStorageService.presignDownload(
                bucket,
                key,
                minutes
        );
    }

    // ==========================================================
    // 🗑 DELETE MEDIA
    // ==========================================================

    public void deletePostMedia(String key) {

        validateKey(key);

        core.delete(bucket, key);
    }

    // ==========================================================
    // 🔒 VALIDATION
    // ==========================================================

    private void validateContentType(String contentType) {

        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Unsupported file type");
        }
    }

    private void validateFileSize(long fileSize) {

        if (fileSize <= 0 || fileSize > MAX_UPLOAD_SIZE) {
            throw new RuntimeException("File exceeds maximum size of 500MB");
        }
    }

    private void validateFileName(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Filename is empty");
        }

        // Extract only name (extra safety)
        fileName = Paths.get(fileName).getFileName().toString();

        // Allow more realistic names
        if (!fileName.matches("^[a-zA-Z0-9._\\-() ]+$")) {
            throw new RuntimeException("Invalid filename format");
        }

        // Ensure extension exists
        if (!fileName.contains(".")) {
            throw new RuntimeException("Missing file extension");
        }
    }

    private void validateKey(String key) {

        if (key == null || !key.startsWith(BASE_PREFIX)) {
            throw new RuntimeException("Invalid media key");
        }
    }

    private String sanitizeFileName(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Invalid filename");
        }

        return fileName
                .replaceAll("[^a-zA-Z0-9\\.\\-_]", "_")
                .toLowerCase();
    }
}
