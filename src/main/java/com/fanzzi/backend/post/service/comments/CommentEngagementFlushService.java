package com.fanzzi.backend.post.service.comments;

import com.fanzzi.backend.post.repository.PostCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentEngagementFlushService {

    private final StringRedisTemplate redis;
    private final PostCommentRepository commentRepo;

    private static final String ACTIVE = "comment:active";
    private static final String LIKE_COUNT = "comment:likes:count:";
    private static final String REPLY_COUNT = "comment:replies:count:";
    private static final String LOCK_KEY = "comment:flush:lock";

    private static final int MAX_BATCH = 50;
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    // =====================================
    // 🔥 FLUSH (DISTRIBUTED SAFE)
    // =====================================
    @Scheduled(fixedRate = 30000)
    public void flush() {

        // =====================================
        // 🔒 DISTRIBUTED LOCK
        // =====================================
        Boolean locked = redis.opsForValue()
                .setIfAbsent(LOCK_KEY, "1", LOCK_TTL);

        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {

            Set<String> commentIds = redis.opsForSet().members(ACTIVE);

            if (commentIds == null || commentIds.isEmpty()) {
                return;
            }

            int processed = 0;

            for (String commentId : commentIds) {

                if (processed >= MAX_BATCH) break;

                try {

                    // =====================================
                    // 🔥 ATOMIC READ + DELETE
                    // =====================================
                    String likeVal =
                            redis.opsForValue().getAndDelete(LIKE_COUNT + commentId);

                    String replyVal =
                            redis.opsForValue().getAndDelete(REPLY_COUNT + commentId);

                    long likes = parse(likeVal);
                    long replies = parse(replyVal);

                    // =====================================
                    // DB UPDATE
                    // =====================================
                    if (likes > 0) {
                        commentRepo.incrementLikes(commentId, likes);
                    }

                    if (replies > 0) {
                        commentRepo.incrementReplies(commentId, replies);
                    }

                    // =====================================
                    // CLEAN ACTIVE SET
                    // =====================================
                    redis.opsForSet().remove(ACTIVE, commentId);

                } catch (Exception e) {
                    log.warn("Comment flush failed commentId={}", commentId, e);
                }

                processed++;
            }

            log.debug("Comment flush processed={}", processed);

        } finally {
            redis.delete(LOCK_KEY);
        }
    }

    // =====================================
    // SAFE PARSE
    // =====================================
    private long parse(String value) {

        if (value == null) return 0;

        try {
            long v = Long.parseLong(value);
            return v > 0 ? v : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}