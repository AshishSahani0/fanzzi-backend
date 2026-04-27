package com.fanzzi.backend.channel.status.repository;

import com.fanzzi.backend.channel.status.dto.ChannelStatusResponse;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChannelStatusAggregationRepositoryImpl
        implements ChannelStatusAggregationRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<ChannelStatusResponse> findActiveStatusesWithViews(
            String channelId,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        MatchOperation match = Aggregation.match(
                Criteria.where("channelId").is(channelId)
                        .and("deleted").is(false)
                        .and("expiresAt").gt(Instant.now())
        );

        LookupOperation lookupViews =
                LookupOperation.newLookup()
                        .from("channel_status_views")
                        .localField("_id")
                        .foreignField("statusId")
                        .as("views");

        AddFieldsOperation viewCount =
                Aggregation.addFields()
                        .addField("viewCount")
                        .withValue(
                                ArrayOperators.Size.lengthOfArray("views")
                        ).build();

        ProjectionOperation projection =
                Aggregation.project()
                        .andExpression("_id").as("id")
                        .and("type").as("type")
                        .and("text").as("text")
                        .and("createdAt").as("createdAt")
                        .and("expiresAt").as("expiresAt")
                        .and("viewCount").as("viewCount")
                        .and("media").as("media");


        Aggregation aggregation = Aggregation.newAggregation(
                match,
                lookupViews,
                viewCount,
                projection,
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt")),
                Aggregation.skip((long) page * size),
                Aggregation.limit(size)
        );

        List<ChannelStatusResponse> statuses =
                mongoTemplate.aggregate(
                        aggregation,
                        "channel_status",
                        ChannelStatusResponse.class
                ).getMappedResults();

        return new PageImpl<>(statuses, pageable, statuses.size());
    }
}