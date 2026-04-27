package com.fanzzi.backend.post.repository;

import com.fanzzi.backend.post.model.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface PostCommentRepository
        extends MongoRepository<PostComment, String> {

    // ================================
    // LOAD COMMENTS (PINNED + RANKING)
    // ================================
    @Query("{ 'postId': ?0, 'depth': ?1 }")
    Page<PostComment> findComments(
            String postId,
            int depth,
            Pageable pageable
    );


    boolean existsPinnedByPostId(String postId);

    @Query("update PostComment c set c.pinned=false where c.postId=:postId")
    void unpinAllByPostId(String postId);

    // ================================
    // LOAD REPLIES
    // ================================
    Page<PostComment> findByParentCommentIdOrderByCreatedAtAsc(
            String parentCommentId,
            Pageable pageable
    );

    // ================================
    // LIKE COUNTER
    // ================================
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'likes': ?1 } }")
    void incrementLikes(String commentId, long count);

    // ================================
    // REPLY COUNTER
    // ================================
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'replyCount': ?1 } }")
    void incrementReplies(String commentId, long count);


}