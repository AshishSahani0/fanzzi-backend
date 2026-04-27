package com.fanzzi.backend.channel.status.service;

import com.fanzzi.backend.common.messaging.websocket.service.WsSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelStatusReactionService {

    private final StringRedisTemplate redisTemplate;
    private final WsSendService wsSendService;

    private static final Set<String> ALLOWED =
            Set.of("❤️", "🔥", "😂", "👍", "😮");

    private static final DefaultRedisScript<List> SCRIPT =
            new DefaultRedisScript<>("""
            local userKey = KEYS[1]
            local countKey = KEYS[2]
            local userId = ARGV[1]
            local newReaction = ARGV[2]

            local oldReaction = redis.call('HGET', userKey, userId)

            if oldReaction and oldReaction == newReaction then
                redis.call('HDEL', userKey, userId)
                redis.call('HINCRBY', countKey, newReaction, -1)
                return {nil}
            end

            if oldReaction then
                redis.call('HINCRBY', countKey, oldReaction, -1)
            end

            redis.call('HSET', userKey, userId, newReaction)
            redis.call('HINCRBY', countKey, newReaction, 1)

            return {newReaction}
        """, List.class);

    public void react(String statusId, String channelId, String userId, String reaction) {

        if (statusId == null || channelId == null || userId == null) return;

        reaction = reaction != null ? reaction.trim() : "";
        if (!ALLOWED.contains(reaction)) return;

        String userKey = "status:reactions:user:" + statusId;
        String countKey = "status:reactions:" + statusId;

        try {
            /// 🔥 EXECUTE LUA
            List result = redisTemplate.execute(
                    SCRIPT,
                    List.of(userKey, countKey),
                    userId,
                    reaction
            );

            String userReaction =
                    !result.isEmpty()
                            ? (String) result.get(0)
                            : null;

            /// 🔥 TTL
            redisTemplate.expire(userKey, Duration.ofHours(48));
            redisTemplate.expire(countKey, Duration.ofHours(48));

            /// 🔥 FULL MAP (IMPORTANT)
            Map<Object, Object> raw =
                    redisTemplate.opsForHash().entries(countKey);

            Map<String, Long> all = raw.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> Long.parseLong(e.getValue().toString())
                    ));

            /// 🔥 TOP 3
            Map<String, Long> top = all.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            /// 🔥 REALTIME (IMPROVED)
            wsSendService.sendToChannelSubscribers(
                    channelId,
                    Map.of(
                            "type", "STATUS_REACTION",
                            "statusId", statusId,
                            "reactions", top,
                            "allReactions", all,
                            "userReaction", userReaction
                    )
            );

        } catch (Exception e) {
            log.error("Reaction failed statusId={} userId={}", statusId, userId, e);
        }
    }

    public Map<String, Object> getReactions(String statusId) {

        String countKey = "status:reactions:" + statusId;

        try {
            Map<Object, Object> raw =
                    redisTemplate.opsForHash().entries(countKey);

            Map<String, Long> all = raw.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> Long.parseLong(e.getValue().toString())
                    ));

            return Map.of(
                    "reactions", all
            );

        } catch (Exception e) {
            log.error("getReactions failed", e);
            return Map.of("reactions", Map.of());
        }
    }
}