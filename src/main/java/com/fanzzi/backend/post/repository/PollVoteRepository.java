package com.fanzzi.backend.post.repository;

import com.fanzzi.backend.post.model.PollVote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository
        extends MongoRepository<PollVote, String> {

    List<PollVote> findByPostIdAndUserId(String postId, String userId);

    long countByPostIdAndOptionId(String postId, String optionId);



}