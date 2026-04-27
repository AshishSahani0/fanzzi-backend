package com.fanzzi.backend.live.premium.controller;

import com.fanzzi.backend.live.premium.model.CreatorLivePlan;
import com.fanzzi.backend.live.premium.service.CreatorLivePlanService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/live/premium")
public class CreatorLivePlanController {

    private final CreatorLivePlanService planService;

    public CreatorLivePlanController(CreatorLivePlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/my")
    public CreatorLivePlan getMyPlan(Authentication authentication) {

        String ownerId = authentication.getName();

        return planService.getActivePlan(ownerId);
    }

    @PostMapping("/purchase")
    public CreatorLivePlan purchase(Authentication authentication) {

        String ownerId = authentication.getName();

        return planService.purchasePlan(ownerId);
    }

}