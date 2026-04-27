package com.fanzzi.backend.channel.query;

import com.fanzzi.backend.channel.common.ChannelMapper;
import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.common.dto.PagedResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelSearchService {

    private final ChannelRepository repository;
    private final ChannelMapper mapper;

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_QUERY_LENGTH = 50;

    // =====================================================
    // 🔍 SEARCH PUBLIC (SAFE + FAST + FILTERED)
    // =====================================================
    public PagedResponse<ChannelResponse> searchPublic(
            String userId,
            String query,
            int page,
            int size
    ) {

        String normalized = normalizeQuery(query);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(size, MAX_PAGE_SIZE),
                Sort.by(
                        Sort.Order.desc("memberCount"),   // 🔥 popularity
                        Sort.Order.desc("lastPostAt")     // 🔥 activity boost
                )
        );

        Page<ChannelResponse> result =
                repository
                        .searchPublicFree(
                                normalized,
                                userId,
                                pageable
                        )
                        .map(ch -> mapper.toResponse(ch, userId));

        return PagedResponse.from(result);
    }

    // =====================================================
    // 🔧 NORMALIZE QUERY
    // =====================================================
    private String normalizeQuery(String q) {
        if (q == null) return "";

        String trimmed = q.trim().toLowerCase();

        return trimmed.length() > MAX_QUERY_LENGTH
                ? trimmed.substring(0, MAX_QUERY_LENGTH)
                : trimmed;
    }
}