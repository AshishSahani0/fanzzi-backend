package com.fanzzi.backend.channel.report.repository;

import com.fanzzi.backend.channel.report.model.ChannelReport;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChannelReportRepository
        extends MongoRepository<ChannelReport, String> {

    // =====================================================
    // BASIC CHECKS
    // =====================================================

    boolean existsByChannelIdAndReportedBy(
            String channelId,
            String reportedBy
    );

    // =====================================================
    // CHANNEL REPORT LIST
    // =====================================================

    List<ChannelReport> findByChannelIdOrderByReportedAtDesc(
            String channelId
    );

    // =====================================================
    // REPORT COUNT
    // =====================================================

    long countByChannelId(String channelId);

    // =====================================================
    // TRUST SCORE AGGREGATION
    // =====================================================

    @Aggregation(pipeline = {
            "{ $match: { channelId: ?0 } }",
            "{ $group: { _id: null, total: { $sum: \"$reporterTrustScore\" } } }"
    })
    Double getTotalTrustScore(String channelId);

    // =====================================================
    // ADMIN BULK DELETE
    // =====================================================

    void deleteByChannelId(String channelId);
}