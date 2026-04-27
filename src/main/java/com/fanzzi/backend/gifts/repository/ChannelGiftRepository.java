package com.fanzzi.backend.gifts.repository;

import com.fanzzi.backend.gifts.model.ChannelGift;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChannelGiftRepository
        extends MongoRepository<ChannelGift, String> {

    List<ChannelGift> findByChannelIdOrderByCreatedAtDesc(String channelId);

}