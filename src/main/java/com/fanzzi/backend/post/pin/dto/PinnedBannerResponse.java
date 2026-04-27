package com.fanzzi.backend.post.pin.dto;

import com.fanzzi.backend.post.dto.PostResponse;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PinnedBannerResponse {

    private int count;
    private PostResponse latest;
    private Instant lastPinnedAt;

}