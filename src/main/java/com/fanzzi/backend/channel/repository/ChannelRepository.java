package com.fanzzi.backend.channel.repository;

import com.fanzzi.backend.channel.enums.ChannelVisibility;
import com.fanzzi.backend.channel.model.Channel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;



public interface ChannelRepository
        extends MongoRepository<Channel, String>,
        ChannelRepositoryCustom {

    Optional<Channel> findByIdAndDeletedFalse(String id);

    Optional<Channel> findBySlug(String slug);

    boolean existsByIdAndOwnerId(String id, String ownerId);

    Optional<Channel> findByInviteToken(String token);

    List<Channel> findAllByIdInAndDeletedFalse(List<String> ids);


    List<Channel> findByIdInAndDeletedFalse(List<String> ids);

    @Query(value = "{ '_id': ?0 }", fields = "{ 'postSeq': 1 }")
    Channel findSeqOnly(String channelId);



    Page<Channel> findByVisibilityAndDiscoverableTrueAndDeletedFalseAndNameLowerContaining(
            ChannelVisibility visibility,
            String nameLower,
            Pageable pageable
    );

    Page<Channel>findByVisibilityAndDiscoverableTrueAndDeletedFalse(
            ChannelVisibility visibility,
            Pageable pageable
    );

    @Query(value = "{ 'ownerId': ?0, 'deleted': false, '_id': { $nin: ?1 } }")
    Page<Channel> findMyChannelsExcludingArchived(
            String userId,
            List<String> archivedIds,
            Pageable pageable
    );

    @Query(value = "{ 'deleted': false, '_id': { $in: ?0, $nin: ?1 } }")
    Page<Channel> findJoinedChannelsExcludingArchived(
            List<String> joinedIds,
            List<String> archivedIds,
            Pageable pageable
    );


    @Query("{ '_id': ?0 }")
    @Update("""
{
  '$inc': { 'postCount': 1 },
  '$set': { 'lastPostAt': ?1, 'updatedAt': ?1 }
}
""")
    void incrementPostStats(String channelId, Instant now);





    @Query("{ '_id': ?0, 'postCount': { $gt: 0 } }")
    @Update("""
        {
          '$inc': { 'postCount': -1 }
        }
        """)
    void decrementPostCount(String channelId);

    @Query("{ '_id': ?0, 'postCount': { $gt: 0 } }")
    @Update("""
{
  '$inc': { 'postCount': ?1 }
}
""")
    void decrementPostCountBy(String channelId, long negativeValue);




    @Query("{ '_id': ?0, 'memberCount': { $gt: 0 } }")
    @Update("{ '$inc': { 'memberCount': -1 } }")
    void decrementMemberCountIfPositive(String channelId);

    @Query(value = "{ '_id': ?0, 'deleted': false }", fields = "{ 'memberCount': 1 }")
    Channel findMemberCountOnly(String channelId);


    @Query("""
{
  $and: [
    { ownerId: { $ne: ?1 } },
    { visibility: 'PUBLIC' },
    { discoverable: true },
    { deleted: false },
    { type: 'FREE' },
    { moderationStatus: 'NORMAL' },
    {
      nameLower: { $regex: '^?0', $options: 'i' }
    }
  ]
}
""")
    Page<Channel> searchPublicFree(
            String query,
            String userId,
            Pageable pageable
    );

}