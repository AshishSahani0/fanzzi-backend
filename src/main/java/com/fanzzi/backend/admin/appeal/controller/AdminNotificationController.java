package com.fanzzi.backend.admin.appeal.controller;

import com.fanzzi.backend.admin.appeal.model.AdminNotification;
import com.fanzzi.backend.admin.appeal.repository.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationRepository repository;

    @GetMapping
    public Page<AdminNotification> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return repository.findByReadFalse(PageRequest.of(page, size));
    }

    @PostMapping("/{id}/read")
    public void markAsRead(@PathVariable String id) {

        repository.findById(id).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }
}
