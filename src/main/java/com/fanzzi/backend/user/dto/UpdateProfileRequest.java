package com.fanzzi.backend.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    private String firstName;
    private String lastName;
    private String bio;

    private String userName;
    private String email;
    private LocalDate dateOfBirth;
    private String profileImageKey;
}
