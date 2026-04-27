package com.fanzzi.backend.admin.audit.service;

import com.fanzzi.backend.admin.audit.model.AdminAuditLog;
import com.fanzzi.backend.admin.audit.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository repository;

    public void log(String action, String targetId, String details) {

        AdminAuditLog log = new AdminAuditLog();
        log.setAdminId("ADMIN");
        log.setAction(action);
        log.setTargetId(targetId);
        log.setDetails(details);

        repository.save(log);
    }
}
