package com.fanzzi.backend.admin.appeal.repository;

import com.fanzzi.backend.admin.appeal.model.AdminNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdminNotificationRepository
        extends MongoRepository<AdminNotification, String> {

    Page<AdminNotification> findByReadFalse(Pageable pageable);

    long countByReadFalse();
}
