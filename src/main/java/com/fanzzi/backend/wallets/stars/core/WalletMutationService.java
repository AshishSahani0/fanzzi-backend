package com.fanzzi.backend.wallets.stars.core;

import com.fanzzi.backend.wallets.stars.model.UserWallet;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WalletMutationService {

    private final MongoTemplate mongoTemplate;

    public void addPurchasedStars(String userId, long amount) {

        Query query = Query.query(Criteria.where("userId").is(userId));

        Update update = new Update()
                .inc("purchasedStars", amount)
                .inc("lifetimePurchased", amount)
                .set("updatedAt", Instant.now())
                .setOnInsert("createdAt", Instant.now());

        mongoTemplate.upsert(query, update, UserWallet.class);
    }

    public boolean deductPurchasedStars(String userId, long amount) {

        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("purchasedStars").gte(amount)
        );

        Update update = new Update()
                .inc("purchasedStars", -amount)
                .set("updatedAt", Instant.now());

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                UserWallet.class
        ) != null;
    }

    public void creditEarnings(String userId, long amount) {

        Query query = Query.query(Criteria.where("userId").is(userId));

        Update update = new Update()
                .inc("earnedStars", amount)
                .inc("lifetimeEarned", amount)
                .set("updatedAt", Instant.now())
                .setOnInsert("createdAt", Instant.now());

        mongoTemplate.upsert(query, update, UserWallet.class);
    }
}