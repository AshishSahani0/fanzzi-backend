package com.fanzzi.backend.channel.membership.repository;

import com.fanzzi.backend.channel.enums.ChannelRole;
import com.fanzzi.backend.channel.membership.model.ChannelMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * ⭐ PRODUCTION-GRADE MEMBERSHIP REPOSITORY
 *
 * - Handles active memberships only (left = false)
 * - Optimized for large-scale systems
 */
public interface ChannelMemberRepository
        extends MongoRepository<ChannelMember, String> {

    // ✅ NEW (IMPORTANT)
    @Query("{ 'userId': ?0, 'left': false, 'role': { $ne: 'OWNER' } }")
    List<ChannelMember> findJoinedOnly(String userId);

    Optional<ChannelMember> findByChannelIdAndUserId(
            String channelId,
            String userId
    );

    @Query(value = "{ 'userId': ?0 }", fields = "{ 'channelId': 1 }")
    List<ChannelMember> findAllByUserId(String userId);

    Page<ChannelMember> findAllByUserId(String userId, Pageable pageable);

    @Query(value = "{ 'userId': ?0, 'left': false }", fields = "{ 'channelId': 1 }")
    List<ChannelMember> findByUserIdAndLeftFalse(String userId);

    boolean existsByUserIdAndChannelIdAndLeftFalse(
            String userId,
            String channelId
    );

    boolean existsByChannelIdAndUserId(
            String channelId,
            String userId
    );

    boolean existsByChannelIdAndUserIdAndLeftFalse(
            String channelId,
            String userId
    );






    void deleteByChannelIdAndUserId(
            String channelId,
            String userId
    );

}