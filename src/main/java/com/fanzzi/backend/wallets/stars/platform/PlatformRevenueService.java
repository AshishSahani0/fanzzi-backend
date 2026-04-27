package com.fanzzi.backend.wallets.stars.platform;

import com.fanzzi.backend.wallets.platform.PlatformWallet;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PlatformRevenueService {

    private final MongoTemplate mongoTemplate;

    public void credit(long amount) {

        Query query = Query.query(Criteria.where("_id").is("MAIN"));

        Update update = new Update()
                .inc("totalRevenue", amount)
                .inc("lifetimeRevenue", amount)
                .set("updatedAt", Instant.now())
                .setOnInsert("createdAt", Instant.now());

        mongoTemplate.upsert(query, update, PlatformWallet.class);
    }
}