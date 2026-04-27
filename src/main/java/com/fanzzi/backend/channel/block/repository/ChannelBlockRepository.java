package com.fanzzi.backend.channel.block.repository;

import com.fanzzi.backend.channel.block.model.ChannelBlock;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelBlockRepository
        extends MongoRepository<ChannelBlock, String> {

    // =============================
    // 🔒 CHECK BLOCK STATUS
    // =============================

    boolean existsByChannelIdAndUserId(
            String channelId,
            String userId
    );

    Optional<ChannelBlock> findByChannelIdAndUserId(
            String channelId,
            String userId
    );

    // =============================
    // 🔓 UNBLOCK
    // =============================

    void deleteByChannelIdAndUserId(
            String channelId,
            String userId
    );

    // =============================
    // 📋 USER BLOCK LIST
    // =============================

    List<ChannelBlock> findByUserId(String userId);

    // ⭐ OPTIONAL — Useful for admin tools
    List<ChannelBlock> findByChannelId(String channelId);
}