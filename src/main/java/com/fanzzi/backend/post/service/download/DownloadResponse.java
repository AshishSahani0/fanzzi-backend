package com.fanzzi.backend.post.service.download;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DownloadResponse {

    private List<String> downloadUrls;
    private int count;
    private boolean isMultiple;
}