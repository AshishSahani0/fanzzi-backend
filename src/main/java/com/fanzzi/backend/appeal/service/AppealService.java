package com.fanzzi.backend.appeal.service;

import com.fanzzi.backend.admin.appeal.model.AdminNotification;
import com.fanzzi.backend.admin.appeal.repository.AdminNotificationRepository;
import com.fanzzi.backend.appeal.dto.CreateAppealRequest;
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
public class AppealService {

    private final BanAppealRepository appealRepository;
    private final UserRepository userRepository;
    private final AdminNotificationRepository notificationRepository;

    // =====================================================
    // 📝 CREATE BAN APPEAL
    // =====================================================
    @Transactional
    public BanAppeal createAppeal(
            String userId,
            CreateAppealRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found"
                ));

        if (!user.isBanned()) {
            throw new ApiException(
                    ErrorCode.NOT_BANNED,
                    "Only banned users can submit appeals"
            );
        }

        if (appealRepository.existsByUserIdAndStatus(
                userId,
                AppealStatus.PENDING
        )) {
            throw new ApiException(
                    ErrorCode.APPEAL_EXISTS,
                    "You already have a pending appeal"
            );
        }

        if (request.getMessage() == null ||
                request.getMessage().isBlank()) {

            throw new ApiException(
                    ErrorCode.BAD_REQUEST,
                    "Appeal message is required"
            );
        }

        // =====================================================
        // 🧾 CREATE APPEAL
        // =====================================================
        BanAppeal appeal = new BanAppeal();

        appeal.setUserId(userId);
        appeal.setPhone(user.getPhone());
        appeal.setMessage(request.getMessage());

        appeal.setBanReason(user.getBanReason());
        appeal.setBannedAt(user.getBannedAt());

        appeal.setStatus(AppealStatus.PENDING);
        appeal.setCreatedAt(Instant.now());

        BanAppeal saved = appealRepository.save(appeal);

        // =====================================================
        // 🔔 ADMIN NOTIFICATION
        // =====================================================
        AdminNotification notification = new AdminNotification();

        notification.setType("BAN_APPEAL");
        notification.setMessage(
                "New ban appeal from user: " + userId
        );
        notification.setCreatedAt(Instant.now());

        notificationRepository.save(notification);

        return saved;
    }
}