package com.fanzzi.backend.channel.delete;

import com.fanzzi.backend.channel.event.ChannelEvent;
import com.fanzzi.backend.channel.event.ChannelEventPublisher;
import com.fanzzi.backend.channel.event.ChannelEventType;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelRestoreService {

    private final MongoTemplate mongoTemplate;
    private final ChannelEventPublisher eventPublisher;

    public Document getChannel(String channelId) {
        return mongoTemplate.findById(channelId, Document.class, "channels");
    }

    public RestoreResult restore(String channelId, String userId) {

        Document channel = mongoTemplate.findById(channelId, Document.class, "channels");

        if (channel == null) {
            throw new RuntimeException("Channel not found");
        }


        Object deletedObj = channel.get("deleted");
        boolean deleted = deletedObj instanceof Boolean && (Boolean) deletedObj;

        if (!deleted) {
            throw new RuntimeException("Channel is not deleted");
        }

        String ownerId = channel.getString("ownerId");

        if (!userId.equals(ownerId)) {
            throw new RuntimeException("Not owner");
        }

        Object deadlineObj = channel.get("deleteScheduledAt");

        if (!(deadlineObj instanceof java.util.Date)) {
            throw new RuntimeException("Restore deadline missing");
        }

        long deadline = ((java.util.Date) deadlineObj).getTime();
        long now = System.currentTimeMillis();

        long remainingMs = deadline - now;

        if (remainingMs <= 0) {
            return new RestoreResult(
                    false,
                    0,
                    0,
                    true // expired
            );
        }

        long remainingDays = remainingMs / (1000 * 60 * 60 * 24);

        Query query = Query.query(
                Criteria.where("_id").is(channelId)
                        .and("ownerId").is(userId)
                        .and("deleted").is(true)
        );

        Update update = new Update()
                .set("deleted", false)
                .unset("deletedAt")
                .unset("deleteScheduledAt")
                .set("discoverable", true);

        var result = mongoTemplate.updateFirst(query, update, "channels");

        if (result.getModifiedCount() != 1) {
            throw new RuntimeException("Restore failed");
        }

        mongoTemplate.updateMulti(
                Query.query(Criteria.where("channelId").is(channelId)),
                new Update().set("deleted", false).unset("deletedAt"),
                "channel_members"
        );

        mongoTemplate.updateMulti(
                Query.query(Criteria.where("channelId").is(channelId)),
                new Update().set("deleted", false).unset("deletedAt"),
                "channel_status"
        );


        eventPublisher.publish(
                new ChannelEvent(
                        channelId,
                        userId,
                        ChannelEventType.RESTORE,
                        channel
                )
        );

        return new RestoreResult(
                true,
                remainingMs,
                remainingDays,
                false
        );
    }
}