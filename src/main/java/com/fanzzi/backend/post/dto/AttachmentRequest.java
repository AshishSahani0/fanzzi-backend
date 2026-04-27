package com.fanzzi.backend.post.dto;

import com.fanzzi.backend.post.enums.AttachmentType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AttachmentRequest {

    @NotBlank(message = "Storage key is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9/_\\-.]+$",
            message = "Invalid storage key format"
    )
    private String key;

    @NotNull(message = "Attachment type is required")
    private AttachmentType type;

    @Size(max = 255, message = "File name too long")
    private String fileName;

    @Min(value = 1, message = "File size must be greater than 0")
    @Max(value = 50_000_000, message = "File too large")
    private long fileSize;

    @Pattern(
            regexp = "^(image|video|audio)/[a-zA-Z0-9.+-]+$",
            message = "Invalid mime type"
    )
    private String mimeType;

    public boolean isVideo() {
        return type == AttachmentType.VIDEO;
    }

    public boolean isImage() {
        return type == AttachmentType.IMAGE;
    }
}