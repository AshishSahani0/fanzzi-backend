package com.fanzzi.backend.post.service.comments;

import com.fanzzi.backend.post.dto.CommentCreatedEvent;
import com.fanzzi.backend.post.model.PostComment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentStreamListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisCommentEngagementService redisService;

    private static final String DEST_PREFIX = "/topic/posts/";

    // =====================================
    // 🔥 REALTIME COMMENT STREAM
    // =====================================
    @Async
    @EventListener
    public void handle(CommentCreatedEvent event) {

        if (event == null || event.postId() == null || event.comment() == null) {
            return;
        }

        try {

            PostComment comment = event.comment();

            // =====================================
            // 🔥 MERGE REDIS ENGAGEMENT
            // =====================================
            long likes = redisService.getLikeCount(comment.getId());
            long replies = redisService.getReplyCount(comment.getId());

            comment.setLikes(comment.getLikes() + likes);
            comment.setReplyCount(comment.getReplyCount() + replies);

            // =====================================
            // RESPONSE
            // =====================================
            CommentRealtimeResponse response =
                    new CommentRealtimeResponse(
                            event.postId(),
                            comment,
                            Instant.now().toEpochMilli()
                    );

            // =====================================
            // SEND
            // =====================================
            messagingTemplate.convertAndSend(
                    DEST_PREFIX + event.postId() + "/comments",
                    response
            );

        } catch (Exception e) {
            log.warn("Comment realtime failed postId={}", event.postId(), e);
        }
    }
}