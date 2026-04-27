package com.fanzzi.backend.user.dto;

import lombok.Data;

@Data
public class PhoneVerifyRequest {
    private String phone;
    private String otp;
}
