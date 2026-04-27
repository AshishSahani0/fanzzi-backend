package com.fanzzi.backend.post.pin.repository;

import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PinnedPostQueryRepository {

    private final ChannelPostRepository postRepository;

    public List<ChannelPost> findPinnedPosts(String channelId) {
        return postRepository
                .findByChannelIdAndDeletedFalseAndPinnedTrueOrderByPinnedAtDesc(channelId);
    }
}