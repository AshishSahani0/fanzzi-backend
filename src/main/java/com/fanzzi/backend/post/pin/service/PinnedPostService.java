package com.fanzzi.backend.post.pin.service;

import com.fanzzi.backend.post.pin.dto.PinnedBannerResponse;
import com.fanzzi.backend.post.pin.dto.PinnedListResponse;

public interface PinnedPostService {

    PinnedBannerResponse getPinnedBanner(String channelId);

    PinnedListResponse getAllPinnedPosts(String channelId);
}