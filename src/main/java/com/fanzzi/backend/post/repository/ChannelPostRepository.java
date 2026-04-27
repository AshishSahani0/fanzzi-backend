package com.fanzzi.backend.post.repository;

import com.fanzzi.backend.post.model.ChannelPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.List;

public interface ChannelPostRepository
        extends MongoRepository<ChannelPost, String> {

    // ======================================================
    // LATEST POSTS
    // ======================================================
    @Query(value = """
{
  'channelId': ?0,
  'deleted': false
}
""", sort = "{ 'seq': -1 }")
    List<ChannelPost> findLatestPosts(
            String channelId,
            Pageable pageable
    );

    // ======================================================
    // PAGINATION (FIXED)
    // ======================================================
    List<ChannelPost> findByChannelIdAndSeqLessThanAndDeletedFalseOrderBySeqDesc(
            String channelId,
            long beforeSeq,
            Pageable pageable
    );

    // ======================================================
    // RECOVERY (FIXED FROM JPA → MONGO)
    // ======================================================
    @Query("""
{
  'channelId': ?0,
  'seq': { $gt: ?1 },
  'deleted': false
}
""")
    List<ChannelPost> findPostsAfterSeq(
            String channelId,
            long seq,
            Pageable pageable
    );

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'deleted': true, 'status': 'DELETED', 'updatedAt': ?1 } }")
    void softDeletePost(String postId, Instant now);


    @Query("{ 'channelId': ?0, '_id': { $in: ?1 } }")
    @Update("{ '$set': { 'deleted': true, 'status': 'DELETED', 'updatedAt': ?2 } }")
    void softDeleteMultiple(String channelId, List<String> postIds, Instant now);


    List<ChannelPost> findByChannelIdAndDeletedFalseAndPinnedTrueOrderByPinnedAtDesc(
            String channelId
    );



    List<ChannelPost> findByChannelIdAndBucketIdAndDeletedFalseOrderBySeqDesc(
            String channelId,
            int bucketId,
            Pageable pageable
    );


    List<ChannelPost> findByChannelIdAndBucketIdAndSeqLessThanAndDeletedFalseOrderBySeqDesc(
            String channelId,
            int bucketId,
            long seq,
            Pageable pageable
    );


    @Query(value = "{ '_id': ?0, 'poll.options.optionId': ?1 }")
    @Update("{ '$inc': { 'poll.options.$.votes': 1, 'poll.totalVotes': 1 } }")
    void incrementVote(String postId, String optionId);
}