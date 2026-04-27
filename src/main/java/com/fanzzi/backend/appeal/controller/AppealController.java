package com.fanzzi.backend.appeal.controller;

import com.fanzzi.backend.appeal.dto.CreateAppealRequest;
import com.fanzzi.backend.appeal.model.BanAppeal;
import com.fanzzi.backend.appeal.repository.BanAppealRepository;
import com.fanzzi.backend.appeal.service.AppealService;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;
    private final BanAppealRepository appealRepository;

    @PostMapping
    public BanAppeal createAppeal(@RequestBody CreateAppealRequest request) {

        String userId = SecurityUtil.getCurrentUserId();

        return appealService.createAppeal(userId, request);
    }

    @GetMapping
    public List<BanAppeal> myAppeals() {

        String userId = SecurityUtil.getCurrentUserId();

        return appealRepository.findByUserId(userId);
    }
}
