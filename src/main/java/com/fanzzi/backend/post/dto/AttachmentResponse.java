package com.fanzzi.backend.post.dto;

import com.fanzzi.backend.post.enums.AttachmentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentResponse {


    private String key;

    private AttachmentType type;

    private String url;
    private String thumbnailUrl;
    private String previewUrl;

    private Integer width;
    private Integer height;

    private Integer duration;

    private String fileName;
    private Long fileSize;
    private String mimeType;


    public String getDisplayUrl() {
        if (thumbnailUrl != null) return thumbnailUrl;
        if (previewUrl != null) return previewUrl;
        return url;
    }

    public boolean isVideo() {
        return type == AttachmentType.VIDEO;
    }

    public boolean isImage() {
        return type == AttachmentType.IMAGE;
    }

    public boolean hasPreview() {
        return previewUrl != null;
    }
}