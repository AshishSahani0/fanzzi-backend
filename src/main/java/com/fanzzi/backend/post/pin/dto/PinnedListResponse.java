package com.fanzzi.backend.post.pin.dto;

import com.fanzzi.backend.post.dto.PostResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PinnedListResponse {

    private int count;
    private List<PostResponse> posts;
}