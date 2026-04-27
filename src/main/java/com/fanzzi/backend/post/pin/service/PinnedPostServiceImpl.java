package com.fanzzi.backend.post.pin.service;

import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.pin.dto.PinnedBannerResponse;
import com.fanzzi.backend.post.pin.dto.PinnedListResponse;
import com.fanzzi.backend.post.pin.repository.PinnedPostQueryRepository;
import com.fanzzi.backend.post.service.mapping.PostResponseMapper;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinnedPostServiceImpl implements PinnedPostService {

    private final PinnedPostQueryRepository pinnedRepository;
    private final PostResponseMapper mapper;
    private final StringRedisTemplate redis;

    private static final String CACHE_KEY = "channel:pinned:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private String key(String channelId) {
        return CACHE_KEY + channelId;
    }

    // =====================================
    // 🔥 PINNED BANNER
    // =====================================
    @Override
    public PinnedBannerResponse getPinnedBanner(String channelId) {

        try {

            List<ChannelPost> pinnedPosts =
                    pinnedRepository.findPinnedPosts(channelId);

            if (pinnedPosts == null || pinnedPosts.isEmpty()) {
                return PinnedBannerResponse.builder()
                        .count(0)
                        .latest(null)
                        .lastPinnedAt(null)
                        .build();
            }

            ChannelPost latestPinned = pinnedPosts.get(0);

            String userId = SecurityUtil.getCurrentUserId();

            PostResponse latest =
                    mapper.map(latestPinned, 0, userId);

            return PinnedBannerResponse.builder()
                    .count(pinnedPosts.size())
                    .latest(latest)
                    .lastPinnedAt(latestPinned.getPinnedAt())
                    .build();

        } catch (Exception e) {
            log.warn("Pinned banner fetch failed channelId={}", channelId, e);
            return PinnedBannerResponse.builder()
                    .count(0)
                    .latest(null)
                    .lastPinnedAt(null)
                    .build();
        }
    }

    // =====================================
    // 🔥 PINNED LIST (WITH CACHE)
    // =====================================
    @Override
    public PinnedListResponse getAllPinnedPosts(String channelId) {

        try {

            // =========================
            // CACHE CHECK
            // =========================
            String cached = redis.opsForValue().get(key(channelId));

            if (cached != null) {
                try {
                    return JsonUtil.fromJson(cached, PinnedListResponse.class);
                } catch (Exception ignored) {}
            }

            // =========================
            // DB FETCH
            // =========================
            List<ChannelPost> pinnedPosts =
                    pinnedRepository.findPinnedPosts(channelId);

            String userId = SecurityUtil.getCurrentUserId();

            List<PostResponse> responses = pinnedPosts.stream()
                    .map(p -> mapper.map(p, 0, userId))
                    .toList();

            PinnedListResponse result =
                    PinnedListResponse.builder()
                            .count(responses.size())
                            .posts(responses)
                            .build();

            // =========================
            // CACHE STORE
            // =========================
            try {
                redis.opsForValue().set(
                        key(channelId),
                        JsonUtil.toJson(result),
                        TTL
                );
            } catch (Exception ignored) {}

            return result;

        } catch (Exception e) {
            log.warn("Pinned list fetch failed channelId={}", channelId, e);
            return PinnedListResponse.builder()
                    .count(0)
                    .posts(List.of())
                    .build();
        }
    }
}