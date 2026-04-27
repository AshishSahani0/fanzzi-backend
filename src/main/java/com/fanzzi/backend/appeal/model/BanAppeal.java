package com.fanzzi.backend.appeal.model;

import com.fanzzi.backend.appeal.enums.AppealStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "ban_appeals")
public class BanAppeal {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String phone;

    private String message;

    private AppealStatus status = AppealStatus.PENDING;

    private String banReason;
    private Instant bannedAt;

    private Instant decidedAt;

    private Instant createdAt = Instant.now();
}
