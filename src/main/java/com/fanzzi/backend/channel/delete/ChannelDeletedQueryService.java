package com.fanzzi.backend.channel.delete;

import com.fanzzi.backend.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelDeletedQueryService {

    private final MongoTemplate mongoTemplate;

    public PagedResponse<DeletedChannelDTO> getDeletedChannels(
            String userId,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "deletedAt")
        );

        Query query = Query.query(
                Criteria.where("ownerId").is(userId)
                        .and("deleted").is(true)
        ).with(pageable);

        List<Document> docs =
                mongoTemplate.find(query, Document.class, "channels");

        // 🔢 total count
        long total = mongoTemplate.count(
                Query.query(
                        Criteria.where("ownerId").is(userId)
                                .and("deleted").is(true)
                ),
                "channels"
        );

        List<DeletedChannelDTO> content = docs.stream()
                .map(this::toDTO)
                .toList();

        // 🔥 Convert to Page → then to PagedResponse
        Page<DeletedChannelDTO> pageData =
                new PageImpl<>(content, pageable, total);

        return PagedResponse.from(pageData); // ✅ FIX
    }

    // =====================================================
    // 🔥 DTO MAPPER WITH REMAINING TIME
    // =====================================================

    private DeletedChannelDTO toDTO(Document doc) {

        String id = doc.getObjectId("_id").toHexString();
        String name = doc.getString("name");

        long deletedAt = 0;
        long remainingMs = 0;
        long remainingDays = 0;
        boolean expired = false;

        Object deletedAtObj = doc.get("deletedAt");
        if (deletedAtObj instanceof java.util.Date date) {
            deletedAt = date.getTime();
        }

        Object deadlineObj = doc.get("deleteScheduledAt");

        if (deadlineObj instanceof java.util.Date date) {

            long deadline = date.getTime();
            long now = System.currentTimeMillis();

            remainingMs = deadline - now;

            if (remainingMs <= 0) {
                expired = true;
                remainingMs = 0;
                remainingDays = 0;
            } else {
                remainingDays = remainingMs / (1000 * 60 * 60 * 24);
            }
        } else {
            expired = true;
        }

        return new DeletedChannelDTO(
                id,
                name,
                deletedAt,
                remainingMs,
                remainingDays,
                expired
        );
    }
}