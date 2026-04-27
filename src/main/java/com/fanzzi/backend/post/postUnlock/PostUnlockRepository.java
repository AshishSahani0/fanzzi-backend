package com.fanzzi.backend.post.postUnlock;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostUnlockRepository
        extends MongoRepository<PostUnlock, String> {

    // =====================================
    // 🔍 BASIC LOOKUPS
    // =====================================

    Optional<PostUnlock> findByUserIdAndPostId(
            String userId,
            String postId
    );

    boolean existsByUserIdAndPostId(
            String userId,
            String postId
    );

    List<PostUnlock> findByUserIdAndPostIdIn(
            String userId,
            List<String> postIds
    );

    void deleteByUserIdAndPostId(
            String userId,
            String postId
    );

    // =====================================
    // 🚀 NEW: USER PURCHASE HISTORY
    // =====================================

    List<PostUnlock> findByUserIdOrderByUnlockedAtDesc(String userId);

    // =====================================
    // 🚀 NEW: CHANNEL ANALYTICS
    // =====================================

    List<PostUnlock> findByChannelId(String channelId);

    long countByChannelId(String channelId);

    // =====================================
    // 🚀 NEW: ONLY ACTIVE (NON-REFUNDED)
    // =====================================

    @Query("{ 'userId': ?0, 'refunded': false }")
    List<PostUnlock> findActiveByUserId(String userId);

    @Query(value = "{ 'userId': ?0, 'postId': ?1, 'refunded': false }")
    Optional<PostUnlock> findActiveByUserIdAndPostId(
            String userId,
            String postId
    );
}