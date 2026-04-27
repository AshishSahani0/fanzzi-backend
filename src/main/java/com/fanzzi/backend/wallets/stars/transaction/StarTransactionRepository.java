package com.fanzzi.backend.wallets.stars.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface StarTransactionRepository
        extends MongoRepository<StarTransaction, String> {

    // 🔥 Paginated transaction history
    Page<StarTransaction> findByUserId(
            String userId,
            Pageable pageable
    );

    // 🔥 Monthly earnings
    List<StarTransaction> findByUserIdAndTypeAndCreatedAtAfter(
            String userId,
            StarTxnType type,
            Instant since
    );

    // 🔥 Channel earnings aggregation
    @Aggregation(pipeline = {
            "{ $match: { userId: ?0, type: 'GIFT_RECEIVED' } }",
            "{ $group: { _id: '$channelId', total: { $sum: '$amount' } } }"
    })
    List<ChannelEarningProjection> aggregateChannelEarnings(String userId);

    boolean existsByUserIdAndReferenceId(String userId, String referenceId);
}