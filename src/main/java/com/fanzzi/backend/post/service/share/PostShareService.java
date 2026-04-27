package com.fanzzi.backend.post.service.share;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.repository.PostStatsRepository;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostShareService {

    private final PostStatsRepository statsRepository;
    private final ChannelPostRepository postRepository;
    private final StringRedisTemplate redis;

    private static final String SHARE_COUNT_KEY = "post:share:count:";
    private static final String SHARE_USERS_KEY = "post:share:users:";
    private static final String ACTIVE_POSTS = "post:share:active";

    private static final Duration TTL = Duration.ofHours(24);

    public void recordShare(String postId) {

        if (postId == null || postId.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid postId");
        }

        ChannelPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        String userId = SecurityUtil.getCurrentUserId();

        try {
            String finalUser = (userId != null) ? userId : "anon";

            String userKey = SHARE_USERS_KEY + postId;

            // 🔥 unique share per user (optional logic)
            Long added = redis.opsForSet().add(userKey, finalUser);

            if (added != null && added > 0) {
                redis.opsForValue().increment(SHARE_COUNT_KEY + postId);
            }

            redis.opsForSet().add(ACTIVE_POSTS, postId);

            redis.expire(userKey, TTL);
            redis.expire(SHARE_COUNT_KEY + postId, TTL);

            log.debug("Share recorded postId={} user={}", postId, finalUser);

        } catch (Exception e) {
            log.warn("Share tracking failed postId={}", postId, e);
        }
    }
}