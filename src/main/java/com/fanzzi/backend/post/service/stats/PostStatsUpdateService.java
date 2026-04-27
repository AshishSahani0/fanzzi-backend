package com.fanzzi.backend.post.service.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PostStatsUpdateService {

    private final MongoTemplate mongoTemplate;

    public void decrementReaction(String postId, String reaction, Instant now) {

        Query query = Query.query(
                Criteria.where("postId").is(postId)
        );

        Update update = new Update()
                .inc("reactions." + reaction, -1)
                .set("updatedAt", now);

        mongoTemplate.updateFirst(query, update, "post_stats");
    }

    public void incrementReaction(String postId, String reaction, Instant now) {

        Query query = Query.query(Criteria.where("postId").is(postId));

        Update update = new Update()
                .inc("reactions." + reaction, 1)
                .set("updatedAt", now);

        mongoTemplate.updateFirst(query, update, "post_stats");
    }
}