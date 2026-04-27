package com.fanzzi.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiMessageResponse {

    private boolean success;
    private String message;

    public static ApiMessageResponse success(String msg) {
        return new ApiMessageResponse(true, msg);
    }
}