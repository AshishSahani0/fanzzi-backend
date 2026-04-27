package com.fanzzi.backend.channel.stats;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelStatsRepository
        extends MongoRepository<ChannelStats, String> {

    /**
     * Atomic increment for member count
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'memberCount': ?1 }, '$set': { 'updatedAt': new Date() } }")
    void incrementMemberCount(String channelId, long value);

    /**
     * Atomic increment for post count
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'postCount': 1 }, '$set': { 'updatedAt': new Date() } }")
    void incrementPostCount(String channelId);

    /**
     * Update lastPostAt timestamp
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastPostAt': ?1, 'updatedAt': new Date() } }")
    void updateLastPostAt(String channelId, java.time.Instant timestamp);



    boolean existsByChannelId(String channelId);
}