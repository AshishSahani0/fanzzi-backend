package com.fanzzi.backend.channel.subscription.repository;

import com.fanzzi.backend.channel.subscription.model.ChannelSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.Optional;

public interface ChannelSubscriptionRepository
        extends MongoRepository<ChannelSubscription, String> {

    Page<ChannelSubscription> findByChannelIdAndActiveTrue(
            String channelId,
            Pageable pageable
    );

    long countByChannelIdAndActiveTrue(String channelId);


    Optional<ChannelSubscription> findByUserIdAndChannelId(
            String userId,
            String channelId
    );

    void deleteByChannelIdAndUserId(
            String channelId,
            String userId
    );

    long deleteByExpiresAtBefore(Instant now);

    boolean existsByUserIdAndChannelIdAndActiveTrueAndExpiresAtAfter(
            String userId,
            String channelId,
            Instant now
    );

    boolean existsByUserIdAndChannelIdAndActiveTrue(
            String userId,
            String channelId
    );// for cleanup job

    @Query("{ 'expiresAt': { $lt: ?0 }, 'active': true }")
    @Update("""
{
  '$set': {
    'active': false,
    'updatedAt': ?0
  }
}
""")
    long deactivateExpired(Instant now);

    @Query("""
{
  'userId': ?0,
  'channelId': ?1,
  'active': true
}
""")
    Optional<ChannelSubscription> findActiveSubscription(
            String userId,
            String channelId
    );
}

