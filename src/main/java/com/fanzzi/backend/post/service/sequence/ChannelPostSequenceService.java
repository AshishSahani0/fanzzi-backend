package com.fanzzi.backend.post.service.sequence;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPostSequenceService {

    private final StringRedisTemplate redisTemplate;
    private final ChannelRepository repository;

    private static final int MAX_RANGE = 1000;

    private String key(String channelId) {
        return "channel:seq:" + channelId;
    }

    // ==========================================
    // NEXT SINGLE SEQUENCE (REDIS)
    public long nextSeq(String channelId) {

        validateChannelId(channelId);

        String redisKey = key(channelId);
        String lockKey = "lock:" + redisKey;

        try {
            Long seq = redisTemplate.opsForValue().increment(redisKey);

            // ✅ Normal case
            if (seq != null && seq > 1) {
                return seq;
            }

            // ==========================================
            // 🔒 LOCK (only one thread initializes)
            // ==========================================
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1");

            if (Boolean.TRUE.equals(locked)) {
                try {
                    // 🔥 double check (maybe another thread already fixed it)
                    String current = redisTemplate.opsForValue().get(redisKey);
                    if (current != null && Long.parseLong(current) > 1) {
                        return Long.parseLong(current);
                    }

                    // 🔥 sync from DB
                    long dbSeq = Optional.ofNullable(repository.findSeqOnly(channelId))
                            .map(Channel::getLastPostSeq)
                            .orElse(0L);

                    redisTemplate.opsForValue().set(redisKey, String.valueOf(dbSeq));

                    Long newSeq = redisTemplate.opsForValue().increment(redisKey);

                    if (newSeq == null || newSeq <= 0) {
                        throw new IllegalStateException("Invalid sequence generated");
                    }

                    return newSeq;

                } finally {
                    redisTemplate.delete(lockKey); // release lock
                }
            }

            Thread.sleep(50);

            Long retry = redisTemplate.opsForValue().increment(redisKey);

            if (retry == null || retry <= 0) {
                throw new IllegalStateException("Invalid sequence generated");
            }

            return retry;

        } catch (Exception e) {
            log.error("Redis sequence failed channelId={}", channelId, e);
            throw new IllegalStateException("Sequence generation failed", e);
        }
    }

    public long[] allocateRange(String channelId, int count) {

        validateChannelId(channelId);

        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than zero");
        }

        if (count > MAX_RANGE) {
            throw new IllegalArgumentException("Range too large");
        }

        try {
            Long endSeq = redisTemplate.opsForValue().increment(key(channelId), count);

            if (endSeq == null || endSeq <= 0) {
                throw new IllegalStateException("Invalid range allocation");
            }

            long startSeq = endSeq - count + 1;

            long[] seqs = new long[count];
            for (int i = 0; i < count; i++) {
                seqs[i] = startSeq + i;
            }

            log.debug("Redis range [{} - {}] channelId={}", startSeq, endSeq, channelId);

            return seqs;

        } catch (Exception e) {
            log.error("Redis range allocation failed channelId={}", channelId, e);
            throw new IllegalStateException("Sequence range allocation failed", e);
        }
    }


    private void validateChannelId(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("ChannelId cannot be null or empty");
        }
    }
}