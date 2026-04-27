package com.fanzzi.backend.media.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class R2CoreStorageService {

    private final S3Presigner presigner;
    private final S3Client s3;

    // =====================================================
    // 🚀 PRESIGNED UPLOAD (Custom Expiry)
    // =====================================================

    public Map<String, String> presignUpload(
            String bucket,
            String key,
            String contentType,
            int expiryMinutes,
            long fileSize
    ) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(fileSize)
                .build();

        PresignedPutObjectRequest presigned =
                presigner.presignPutObject(
                        PutObjectPresignRequest.builder()
                                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                                .putObjectRequest(request)
                                .build()
                );

        return Map.of(
                "key", key,
                "uploadUrl", presigned.url().toString()
        );
    }

    // =====================================================
    // 🔐 PRESIGNED DOWNLOAD (Custom Expiry)
    // =====================================================

    public String presignDownload(
            String bucket,
            String key,
            int expiryMinutes
    ) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PresignedGetObjectRequest presigned =
                presigner.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                                .getObjectRequest(request)
                                .build()
                );

        return presigned.url().toString();
    }

    // =====================================================
    // 🗑 DELETE
    // =====================================================

    public void delete(String bucket, String key) {

        if (key == null || key.isBlank()) return;

        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}