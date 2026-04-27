package com.fanzzi.backend.admin.appeal.controller;

import com.fanzzi.backend.admin.appeal.dto.AppealDecisionRequest;
import com.fanzzi.backend.admin.appeal.service.AdminAppealService;
import com.fanzzi.backend.appeal.enums.AppealStatus;
import com.fanzzi.backend.appeal.model.BanAppeal;
import com.fanzzi.backend.appeal.repository.BanAppealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/appeals")
@RequiredArgsConstructor
public class AdminAppealController {

    private final AdminAppealService adminAppealService;
    private final BanAppealRepository appealRepository;

    @PostMapping("/{appealId}/decision")
    public void decideAppeal(
            @PathVariable String appealId,
            @RequestBody AppealDecisionRequest request
    ) {
        AppealStatus decision =
                AppealStatus.valueOf(request.getDecision());

        adminAppealService.decideAppeal(appealId, decision);
    }

    @GetMapping
    public Page<BanAppeal> listAppeals(
            @RequestParam AppealStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return appealRepository.findByStatus(
                status,
                PageRequest.of(page, size)
        );
    }
}
