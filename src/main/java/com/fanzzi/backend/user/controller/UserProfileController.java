package com.fanzzi.backend.user.controller;

import com.fanzzi.backend.common.dto.ApiMessageResponse;
import com.fanzzi.backend.user.dto.*;
import com.fanzzi.backend.user.service.UserPhoneService;
import com.fanzzi.backend.user.service.UserProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;
    private final UserPhoneService phoneService;


    @GetMapping
    public UserProfileResponse getMe() {
        return profileService.getMe();
    }


    @PutMapping
    public UserProfileResponse update(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profileService.update(request);
    }


    @PostMapping("/phone/otp")
    public ApiMessageResponse sendOtp(
            @Valid @RequestBody PhoneRequest request
    ) {
        phoneService.sendOtp(request.getPhone());
        return ApiMessageResponse.success("OTP sent");
    }


    @PostMapping("/phone/verify")
    public ApiMessageResponse verifyPhone(
            @Valid @RequestBody PhoneVerifyRequest request
    ) {
        phoneService.verifyAndChange(
                request.getPhone(),
                request.getOtp()
        );

        return ApiMessageResponse.success("Phone updated");
    }
}