package com.fanzzi.backend.post.service.delete;

import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.service.feed.HydratedFeedCacheService;
import com.fanzzi.backend.post.util.PostDeleteRealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteBulkPostsService {

    private final ChannelPostRepository repository;
    private final ChannelRepository channelRepository;
    private final HydratedFeedCacheService cacheService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void deleteBulk(String channelId, List<String> postIds) {

        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        Set<String> uniqueIds = Set.copyOf(postIds);

        // ====================================
        // LOAD POSTS (IMPORTANT)
        // ====================================
        List<ChannelPost> posts = repository.findAllById(uniqueIds);

        if (posts.isEmpty()) return;

        Instant now = Instant.now();

        // ====================================
        // FILTER VALID POSTS
        // ====================================
        List<ChannelPost> validPosts = posts.stream()
                .filter(p -> p.getChannelId().equals(channelId))
                .filter(p -> !p.isDeleted())
                .toList();

        if (validPosts.isEmpty()) return;

        List<String> validIds = validPosts.stream()
                .map(ChannelPost::getId)
                .toList();

        // ====================================
        // DB DELETE
        // ====================================
        repository.softDeleteMultiple(channelId, validIds, now);

        // ====================================
        // CACHE
        // ====================================
        try {
            cacheService.removePosts(channelId, Set.copyOf(validIds));
        } catch (Exception e) {
            log.warn("Bulk cache removal failed channelId={}", channelId, e);
        }

        // ====================================
        // CHANNEL COUNT
        // ====================================
        try {
            channelRepository.decrementPostCountBy(channelId, validIds.size());
        } catch (Exception e) {
            log.error("Failed to decrement post count channelId={}", channelId, e);
        }

        // ====================================
        // REALTIME (WITH SEQ)
        // ====================================
        for (ChannelPost post : validPosts) {
            publisher.publishEvent(
                    new PostDeleteRealtimeEvent(
                            channelId,
                            post.getId(),
                            post.getSeq()
                    )
            );
        }
    }
}