package com.fanzzi.backend.channel.status.repository;

import com.fanzzi.backend.channel.status.model.ChannelStatus;
import com.fanzzi.backend.channel.status.views.ChannelStatusView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.List;

public interface ChannelStatusRepository
        extends MongoRepository<ChannelStatus, String> {


    boolean existsByChannelIdAndDeletedFalseAndExpiresAtAfter(
            String channelId,
            Instant now
    );

    List<ChannelStatus> findByChannelIdAndDeletedFalseAndExpiresAtAfter(
            String channelId,
            Instant now,
            Sort sort
    );


    @Query("{ 'expiresAt': { $lt: ?0 }, 'deleted': false }")
    @Update("{ '$set': { 'deleted': true } }")
    int softExpire(Instant now);



}