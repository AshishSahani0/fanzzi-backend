package com.fanzzi.backend.channel.delete;

import com.fanzzi.backend.channel.event.ChannelEvent;
import com.fanzzi.backend.channel.event.ChannelEventPublisher;
import com.fanzzi.backend.channel.event.ChannelEventType;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChannelDeleteService {

    private final MongoTemplate mongoTemplate;
    private final ChannelEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    private static final long RECOVERY_DAYS = 7;

    public void delete(String channelId, String userId) {

        Instant now = Instant.now();
        Instant deleteAt = now.plusSeconds(RECOVERY_DAYS * 86400);

        Query query = new Query(
                Criteria.where("_id").is(channelId)
                        .and("ownerId").is(userId)
                        .and("deleted").is(false)
        );

        Update update = new Update()
                .set("deleted", true)
                .set("deletedAt", now)
                .set("deleteScheduledAt", deleteAt) // 🔥 restore deadline
                .set("updatedAt", now)
                .set("discoverable", false)
                .set("deleteVersion", now.toEpochMilli())
                .set("deleteReason", "USER_ACTION")
                .unset("slug")
                .unset("inviteToken");

        var result = mongoTemplate.updateFirst(query, update, "channels");

        if (result.getModifiedCount() == 1) {

            publishDeleteEvent(channelId, userId);
            softDeleteRelationsAsync(channelId, now);
            cleanupCache(channelId);

            return;
        }

        throwError(channelId);
    }

    // =====================================================
    // 🔥 SOFT DELETE RELATIONS
    // =====================================================

    @Async
    protected void softDeleteRelationsAsync(String channelId, Instant now) {

        Update softDelete = new Update()
                .set("deleted", true)
                .set("deletedAt", now);

        mongoTemplate.updateMulti(
                Query.query(Criteria.where("channelId").is(channelId)),
                softDelete,
                "channel_members"
        );

        mongoTemplate.updateMulti(
                Query.query(Criteria.where("channelId").is(channelId)),
                softDelete,
                "channel_status"
        );

        // 💰 HARD DELETE SUBSCRIPTIONS
        mongoTemplate.remove(
                Query.query(Criteria.where("channelId").is(channelId)),
                "channel_subscriptions"
        );
    }

    // =====================================================
    // ⚡ CACHE CLEAN
    // =====================================================

    private void cleanupCache(String channelId) {
        try {
            redisTemplate.delete("channel:" + channelId);
            redisTemplate.delete("channel:members:" + channelId);
            redisTemplate.delete("channel:feed:" + channelId);
            redisTemplate.delete("channel:meta:" + channelId);
        } catch (Exception ignored) {
            // never break flow
        }
    }

    // =====================================================
    // 🔥 REALTIME EVENT
    // =====================================================

    @Async
    protected void publishDeleteEvent(String channelId, String userId) {
        try {
            eventPublisher.publish(
                    new ChannelEvent(channelId, userId, ChannelEventType.DELETE, null)
            );
        } catch (Exception ignored) {}
    }

    // =====================================================
    // ❗ ERROR HANDLING (FIXED)
    // =====================================================

    private void throwError(String channelId) {

        Document channel = mongoTemplate.findById(channelId, Document.class, "channels");

        if (channel == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Channel not found");
        }

        // 🔥 SAFE BOOLEAN EXTRACTION
        Object deletedObj = channel.get("deleted");
        boolean deleted = deletedObj instanceof Boolean && (Boolean) deletedObj;

        if (deleted) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Channel already deleted"
            );
        }

        throw new ApiException(
                ErrorCode.FORBIDDEN,
                "Only owner can delete"
        );
    }
}