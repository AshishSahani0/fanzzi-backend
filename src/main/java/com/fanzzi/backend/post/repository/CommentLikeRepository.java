package com.fanzzi.backend.post.repository;


import com.fanzzi.backend.post.model.CommentLike;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CommentLikeRepository
        extends MongoRepository<CommentLike,String> {

    Optional<CommentLike> findByCommentIdAndUserId(
            String commentId,
            String userId
    );

    long countByCommentId(String commentId);
}
