package com.fanzzi.backend.post.service.poll;

import com.fanzzi.backend.post.dto.Poll;
import com.fanzzi.backend.post.dto.PollOption;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PollSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChannelPostRepository postRepository;
    private final PollRedisService redisService;
    private final PollCacheService pollCacheService; // 🔥 NEW

    private static final String DEST_PREFIX = "/topic/poll/";

    @Async
    @EventListener
    public void onVote(PollVoteEvent event) {

        if (event == null || event.postId() == null) return;

        try {

            // =====================================
            // 1️⃣ CACHE FIRST (NO DB HIT)
            // =====================================
            Poll poll = pollCacheService.get(event.postId());

            if (poll == null) {
                // fallback (rare)
                poll = postRepository.findById(event.postId())
                        .map(p -> p.getPoll())
                        .orElse(null);

                if (poll == null) {
                    log.warn("Poll not found postId={}", event.postId());
                    return;
                }

                // 🔥 store in cache
                pollCacheService.put(event.postId(), poll);
            }

            // =====================================
            // 2️⃣ REDIS DELTA (TYPE SAFE)
            // =====================================
            Map<String, Long> redisStats =
                    redisService.getStats(event.postId());

            long total = 0;

            for (PollOption opt : poll.getOptions()) {

                long baseVotes = opt.getVotes();   // DB base (cached)
                long delta = redisStats.getOrDefault(opt.getOptionId(), 0L);

                long finalVotes = baseVotes + delta;

                if (finalVotes < 0) finalVotes = 0;

                opt.setVotes(finalVotes);
                total += finalVotes;
            }

            poll.setTotalVotes(total);

            // =====================================
            // 3️⃣ RESPONSE
            // =====================================
            PollRealtimeResponse response =
                    new PollRealtimeResponse(
                            event.postId(),
                            poll,
                            Instant.now().toEpochMilli()
                    );

            // =====================================
            // 4️⃣ SEND
            // =====================================
            messagingTemplate.convertAndSend(
                    DEST_PREFIX + event.postId(),
                    response
            );

        } catch (Exception e) {
            log.warn("Poll realtime push failed postId={}", event.postId(), e);
        }
    }
}