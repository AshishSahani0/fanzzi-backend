package com.fanzzi.backend.post.repository;

import com.fanzzi.backend.post.model.PostReaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PostReactionRepository
        extends MongoRepository<PostReaction, String> {

    Optional<PostReaction> findByPostIdAndUserId(String postId, String userId);

    long countByPostId(String postId);



}