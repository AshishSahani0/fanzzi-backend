package com.fanzzi.backend.user.service;

import com.fanzzi.backend.channel.event.userprofile.UserEvent;
import com.fanzzi.backend.channel.event.userprofile.UserEventType;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.media.gateway.userprofile.UserMediaGateway;
import com.fanzzi.backend.media.gateway.userprofile.UserProfileCleanupService;
import com.fanzzi.backend.security.SecurityUtil;
import com.fanzzi.backend.user.dto.UpdateProfileRequest;
import com.fanzzi.backend.user.dto.UserProfileResponse;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository repo;
    private final UserMediaGateway userMediaGateway;
    private final UserProfileCleanupService cleanupService;
    private final ApplicationEventPublisher publisher;

    // ==========================================
    // 👤 GET CURRENT PROFILE
    // ==========================================
    public UserProfileResponse getMe() {

        User user = getCurrentUser();

        return map(user);
    }


    public UserProfileResponse update(UpdateProfileRequest req) {

        User user = getCurrentUser();


        if (req.getUserName() != null) {

            String normalized = normalize(req.getUserName());

            validateUsername(normalized);

            if (!normalized.equals(user.getUserNameLower()) &&
                    repo.existsByUserNameLower(normalized)) {

                throw new ApiException(
                        ErrorCode.USERNAME_TAKEN,
                        "Username already taken"
                );
            }

            user.setUserName(req.getUserName().trim());
            user.setUserNameLower(normalized);
        }



        if (req.getFirstName() != null)
            user.setFirstName(clean(req.getFirstName()));

        if (req.getLastName() != null)
            user.setLastName(clean(req.getLastName()));

        if (req.getBio() != null)
            user.setBio(clean(req.getBio()));


        if (req.getEmail() != null) {

            String email = req.getEmail().trim().toLowerCase();

            if (!email.equals(user.getEmail()) &&
                    repo.existsByPhone(email)) {
                throw new ApiException(
                        ErrorCode.EMAIL_ALREADY_EXISTS,
                        "Email already in use"
                );
            }

            user.setEmail(email);
            user.setEmailVerified(false); // reset verification
        }


        if (req.getDateOfBirth() != null) {
            user.setDateOfBirth(req.getDateOfBirth());
        }

        String oldKey = user.getProfileImageKey();

        if (req.getProfileImageKey() != null &&
                !req.getProfileImageKey().equals(oldKey)) {

            user.setProfileImageKey(req.getProfileImageKey());
        }

        user.setUpdatedAt(Instant.now());

        repo.save(user);

        if (oldKey != null && req.getProfileImageKey() != null &&
                !req.getProfileImageKey().equals(oldKey)) {

            cleanupService.deleteOldProfileImage(oldKey);
        }

        UserProfileResponse response = map(user);

        publisher.publishEvent(
                new UserEvent(
                        user.getId(),
                        UserEventType.PROFILE_UPDATE,
                        response
                )
        );

        return response;
    }



    private User getCurrentUser() {
        String userId = SecurityUtil.getCurrentUserId();

        User user = repo.findById(userId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (user.isDeleted()) {
            throw new ApiException(ErrorCode.ACCOUNT_BLOCKED, "Account deleted");
        }

        if (user.isBanned()) {
            throw new ApiException(ErrorCode.ACCOUNT_BLOCKED, "Account banned");
        }

        return user;
    }

    private void validateUsername(String username) {

        if (username.length() < 3 || username.length() > 20) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Username must be 3-20 characters"
            );
        }

        if (!username.matches("^[a-z0-9_.]+$")) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Username can only contain lowercase letters, numbers, _, ."
            );
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }

    private String clean(String value) {
        return value.trim();
    }


    private UserProfileResponse map(User user) {
        String profileUrl = null;

        if (user.getProfileImageKey() != null) {
            profileUrl = userMediaGateway.getUserProfileUrl(user.getProfileImageKey());
        }
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getUserName(),
                user.isHidePhone() ? null : user.getPhone(),
                user.getEmail(),
                profileUrl,
                user.getDateOfBirth()
        );
    }
}