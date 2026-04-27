package com.fanzzi.backend.admin.audit.repository;

import com.fanzzi.backend.admin.audit.model.AdminAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdminAuditLogRepository
        extends MongoRepository<AdminAuditLog, String> {
}
