package com.fanzzi.backend.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminAccessController {

    @GetMapping("/check")
    public Map<String, Boolean> checkAdminAccess() {
        // If request reaches here, OwnerAdminFilter already approved
        return Map.of("admin", true);
    }
}

