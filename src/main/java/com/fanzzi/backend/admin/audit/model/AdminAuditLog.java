package com.fanzzi.backend.admin.audit.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "admin_audit_logs")
public class AdminAuditLog {

    @Id
    private String id;

    private String adminId;

    private String action;

    private String targetId;

    private String details;

    private Instant createdAt = Instant.now();
}
