package com.fanzzi.backend.channel.repository;

import com.fanzzi.backend.channel.model.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class ChannelRepositoryImpl implements ChannelRepositoryCustom {

    private final MongoTemplate mongo;

    private Query byId(String id) {
        return Query.query(Criteria.where("_id").is(id));
    }

    // =====================================================
    // 👥 MEMBER COUNT
    // =====================================================

    @Override
    public void incrementMemberCount(String channelId) {
        updateInc(channelId, "memberCount");
    }

    @Override
    public void decrementMemberCount(String channelId) {
        updateDecSafe(channelId, "memberCount");
    }

    // =====================================================
    // ⭐ SUBSCRIBER COUNT
    // =====================================================

    @Override
    public void incrementSubscriberCount(String channelId) {
        updateInc(channelId, "subscriberCount");
    }

    @Override
    public void decrementSubscriberCount(String channelId) {
        updateDecSafe(channelId, "subscriberCount");
    }

    // =====================================================
    // 📝 POST ACTIVITY
    // =====================================================

    @Override
    public void incrementPostCount(String channelId) {

        Query query = byId(channelId);

        Update update = new Update()
                .inc("postCount", 1)
                .set("lastPostAt", Instant.now())
                .set("updatedAt", Instant.now());

        mongo.updateFirst(query, update, Channel.class);
    }

    @Override
    public Long incrementPostSeq(String channelId) {

        Query query = byId(channelId);

        Update update = new Update()
                .inc("lastPostSeq", 1)
                .set("updatedAt", Instant.now());

        FindAndModifyOptions options =
                FindAndModifyOptions.options()
                        .returnNew(true);

        Channel updated = mongo.findAndModify(
                query,
                update,
                options,
                Channel.class
        );

        return updated != null ? updated.getLastPostSeq() : null;
    }

    @Override
    public Long incrementPostSeqBy(String channelId, long amount) {

        Query query = byId(channelId);

        Update update = new Update()
                .inc("lastPostSeq", amount)
                .set("updatedAt", Instant.now());

        FindAndModifyOptions options =
                FindAndModifyOptions.options()
                        .returnNew(true);

        Channel updated = mongo.findAndModify(
                query,
                update,
                options,
                Channel.class
        );

        return updated != null ? updated.getLastPostSeq() : null;
    }

    // =====================================================
    // ⚙️ INTERNAL HELPERS
    // =====================================================

    private void updateInc(String id, String field) {

        Query query = byId(id);

        Update update = new Update()
                .inc(field, 1)
                .set("updatedAt", Instant.now());

        mongo.updateFirst(query, update, Channel.class);
    }

    /**
     * Prevent negative counters
     */
    private void updateDecSafe(String id, String field) {

        Query query = Query.query(
                Criteria.where("_id").is(id)
                        .and(field).gt(0)
        );

        Update update = new Update()
                .inc(field, -1)
                .set("updatedAt", Instant.now());

        mongo.updateFirst(query, update, Channel.class);
    }

    @Override
    public Long decrementMemberCountAndGet(String channelId) {

        Query query = Query.query(
                Criteria.where("_id").is(channelId)
                        .and("memberCount").gt(0)
        );

        Update update = new Update()
                .inc("memberCount", -1)
                .set("updatedAt", Instant.now());

        Channel updated = mongo.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Channel.class
        );

        return updated != null ? updated.getMemberCount() : 0L;
    }

    @Override
    public Long incrementMemberCountAndGet(String channelId) {

        Query query = byId(channelId);

        Update update = new Update()
                .inc("memberCount", 1)
                .set("updatedAt", Instant.now());

        Channel updated = mongo.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Channel.class
        );

        return updated != null ? updated.getMemberCount() : 0L;
    }
}