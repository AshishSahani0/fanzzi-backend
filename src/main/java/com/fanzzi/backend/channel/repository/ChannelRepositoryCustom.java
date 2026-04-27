package com.fanzzi.backend.channel.repository;

public interface ChannelRepositoryCustom {

    void incrementMemberCount(String channelId);

    void decrementMemberCount(String channelId);

    Long decrementMemberCountAndGet(String channelId);
    Long incrementMemberCountAndGet(String channelId);

    void incrementSubscriberCount(String channelId);

    void decrementSubscriberCount(String channelId);

    void incrementPostCount(String channelId);

    // ===============================
    // POST SEQUENCE
    // ===============================

    Long incrementPostSeq(String channelId);

    Long incrementPostSeqBy(String channelId, long amount);
}