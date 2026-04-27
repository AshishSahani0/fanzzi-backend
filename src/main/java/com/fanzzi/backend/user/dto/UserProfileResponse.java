package com.fanzzi.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String bio;

    private String userName;
    private String phone;

    private String email;
    private String profileImageUrl;

    private LocalDate dateOfBirth;
}
