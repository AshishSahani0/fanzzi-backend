package com.fanzzi.backend.post.repository;

import com.fanzzi.backend.post.enums.ReactionType;
import com.fanzzi.backend.post.model.PostStats;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.Map;

public interface PostStatsRepository
        extends MongoRepository<PostStats, String> {

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'views': ?1 }, '$set': { 'updatedAt': ?2 } }")
    void incrementViewsBy(String postId, long count, Instant now);


    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'comments': 1 }, '$set': { 'updatedAt': ?1 } }")
    void incrementComments(String postId, Instant now);

    @Query("{ 'postId': ?0 }")
    @Update("{ '$inc': { 'shares': ?1 } }")
    void incrementShares(String postId, long delta);



    @Query("{ 'postId': ?0 }")
    @Update("""
{
  '$inc': { 'unlocks': 1 },
  '$set': { 'updatedAt': ?1 }
}
""")
    void incrementUnlocks(String postId, Instant now);

    @Query(value = "{ 'postId': ?0 }", fields = "{ 'reactions': 1 }")
    PostStats findReactionsByPostId(String postId);


    @Query(value = "{ 'postId': ?0 }", fields = "{ 'shares': 1 }")
    Long findSharesByPostId(String postId);
}
