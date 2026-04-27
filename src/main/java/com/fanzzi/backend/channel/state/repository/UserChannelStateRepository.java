package com.fanzzi.backend.channel.state.repository;

import com.fanzzi.backend.channel.state.model.UserChannelState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserChannelStateRepository
        extends MongoRepository<UserChannelState, String> {

    Optional<UserChannelState> findByUserIdAndChannelId(
            String userId,
            String channelId
    );

    List<UserChannelState> findByUserIdAndArchivedTrue(String userId);

    List<UserChannelState> findAllByUserIdAndChannelIdIn(
            String userId,
            List<String> channelIds
    );
}