package com.fanzzi.backend.channel.status.repository;

import com.fanzzi.backend.channel.status.dto.ChannelStatusResponse;
import org.springframework.data.domain.Page;

public interface ChannelStatusAggregationRepository {

    Page<ChannelStatusResponse> findActiveStatusesWithViews(
            String channelId,
            int page,
            int size
    );

}