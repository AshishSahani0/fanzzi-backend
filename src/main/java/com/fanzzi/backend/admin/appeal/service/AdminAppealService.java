package com.fanzzi.backend.admin.appeal.service;

import com.fanzzi.backend.admin.audit.service.AdminAuditLogService;
import com.fanzzi.backend.admin.appeal.model.AdminNotification;
import com.fanzzi.backend.admin.appeal.repository.AdminNotificationRepository;
import com.fanzzi.backend.appeal.enums.AppealStatus;
import com.fanzzi.backend.appeal.model.BanAppeal;
import com.fanzzi.backend.appeal.repository.BanAppealRepository;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminAppealService {

    private final BanAppealRepository appealRepository;
    private final UserRepository userRepository;
    private final AdminNotificationRepository notificationRepository;
    private final AdminAuditLogService auditLogService;

    // =====================================================
    // ⚖️ DECIDE APPEAL
    // =====================================================
    @Transactional
    public void decideAppeal(String appealId, AppealStatus decision) {

        BanAppeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.APPEAL_NOT_FOUND,
                        "Appeal not found"
                ));

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new ApiException(
                    ErrorCode.APPEAL_ALREADY_DECIDED,
                    "Appeal already processed"
            );
        }

        // ✅ Update appeal status
        appeal.setStatus(decision);
        appeal.setDecidedAt(Instant.now());
        appealRepository.save(appeal);

        // =====================================================
        // ✅ ACCEPT → UNBAN USER
        // =====================================================
        if (decision == AppealStatus.ACCEPTED) {

            User user = userRepository.findById(appeal.getUserId())
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.USER_NOT_FOUND,
                            "User not found"
                    ));

            user.setBanned(false);
            user.setBanReason(null);
            user.setBannedAt(null);
            user.setUpdatedAt(Instant.now());

            userRepository.save(user);
        }

        // =====================================================
        // 🧾 AUDIT LOG
        // =====================================================
        auditLogService.log(
                "APPEAL_" + decision.name(),
                appeal.getUserId(),
                "appealId=" + appeal.getId()
        );

        // =====================================================
        // 🔔 ADMIN NOTIFICATION
        // =====================================================
        AdminNotification notification = new AdminNotification();

        notification.setType("APPEAL_" + decision.name());
        notification.setMessage(
                "Appeal " + decision.name() +
                        " for user " + appeal.getUserId()
        );
        notification.setCreatedAt(Instant.now());

        notificationRepository.save(notification);
    }
}