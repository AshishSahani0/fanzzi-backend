package com.fanzzi.backend.post.repository;

import com.fanzzi.backend.post.model.PollOptionStat;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PollOptionStatRepository
        extends MongoRepository<PollOptionStat, String> {

    Optional<PollOptionStat> findByPostIdAndOptionId(String postId, String optionId);

    List<PollOptionStat> findByPostId(String postId);
}
