package com.fanzzi.backend.post.service.poll;

import com.fanzzi.backend.post.model.PollOptionStat;
import com.fanzzi.backend.post.repository.PollOptionStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollSyncService {

    private final StringRedisTemplate redis;
    private final PollOptionStatRepository statRepository;
    private final PollCacheService pollCacheService; // 🔥 NEW (optional but recommended)

    private static final String POLL_KEY_PREFIX = "poll:";

    public void syncPoll(String postId) {

        if (postId == null || postId.isBlank()) {
            return;
        }

        String redisKey = POLL_KEY_PREFIX + postId;

        Map<Object, Object> raw =
                redis.opsForHash().entries(redisKey);

        if (raw == null || raw.isEmpty()) {
            return;
        }

        try {

            // =====================================
            // 1️⃣ FETCH EXISTING STATS
            // =====================================
            List<PollOptionStat> existing =
                    statRepository.findByPostId(postId);

            Map<String, PollOptionStat> existingMap = new HashMap<>();
            for (PollOptionStat stat : existing) {
                existingMap.put(stat.getOptionId(), stat);
            }

            List<PollOptionStat> toSave = new ArrayList<>();
            Instant now = Instant.now();

            // =====================================
            // 2️⃣ APPLY DELTA (IMPORTANT FIX)
            // =====================================
            for (Map.Entry<Object, Object> entry : raw.entrySet()) {

                String optionId = entry.getKey().toString();

                long delta;
                try {
                    delta = Long.parseLong(entry.getValue().toString());
                } catch (Exception e) {
                    log.warn("Invalid vote value postId={} optionId={}", postId, optionId);
                    continue;
                }

                if (delta == 0) continue;

                PollOptionStat stat = existingMap.get(optionId);

                if (stat == null) {
                    stat = new PollOptionStat();
                    stat.setPostId(postId);
                    stat.setOptionId(optionId);
                    stat.setVotes(Math.max(delta, 0));
                } else {
                    long newVotes = stat.getVotes() + delta;

                    if (newVotes < 0) newVotes = 0;

                    stat.setVotes(newVotes);
                }

                stat.setUpdatedAt(now);
                toSave.add(stat);
            }

            // =====================================
            // 3️⃣ BULK SAVE
            // =====================================
            if (!toSave.isEmpty()) {
                statRepository.saveAll(toSave);
            }

            // =====================================
            // 4️⃣ CLEAR REDIS (CRITICAL)
            // =====================================
            redis.delete(redisKey);

            // =====================================
            // 5️⃣ REFRESH CACHE
            // =====================================
            pollCacheService.evict(postId);

        } catch (Exception e) {
            log.warn("Poll sync failed postId={}", postId, e);
        }
    }
}