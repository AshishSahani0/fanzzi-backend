package com.fanzzi.backend.live.repository;

import com.fanzzi.backend.live.model.LiveStream;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LiveRepository extends MongoRepository<LiveStream, String> {

    Optional<LiveStream> findByChannelIdAndStatus(String channelId, String status);

}
