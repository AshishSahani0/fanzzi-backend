package com.fanzzi.backend.channel.status.views;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChannelStatusViewRepository
        extends MongoRepository<ChannelStatusView, String> {

    boolean existsByStatusIdAndViewerId(
            String statusId,
            String viewerId
    );

    long countByStatusId(String statusId);

    Page<ChannelStatusView> findByStatusId(
            String statusId,
            Pageable pageable
    );

    List<ChannelStatusView> findTop3ByStatusIdOrderByViewedAtDesc(String statusId);
}