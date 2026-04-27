package com.fanzzi.backend.admin.appeal.dto;

import lombok.Data;

@Data
public class AppealDecisionRequest {

    private String decision;   // ACCEPTED or REJECTED
    private String adminNote;  // optional internal note
}
