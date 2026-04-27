package com.fanzzi.backend.admin.appeal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "admin_notifications")
public class AdminNotification {

    @Id
    private String id;

    @Indexed
    private String type;

    private String message;

    private boolean read = false;

    private Instant createdAt = Instant.now();
}
