package com.fanzzi.backend.live.premium.service;

import com.fanzzi.backend.live.premium.model.CreatorLivePlan;
import com.fanzzi.backend.live.premium.repository.CreatorLivePlanRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreatorLivePlanService {

    private final CreatorLivePlanRepository planRepository;

    public CreatorLivePlanService(CreatorLivePlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public CreatorLivePlan getActivePlan(String ownerId) {
        return planRepository
                .findByOwnerIdAndActive(ownerId, true)
                .orElse(null);
    }

    public CreatorLivePlan purchasePlan(String ownerId) {

        // deactivate old plan
        planRepository.findByOwnerIdAndActive(ownerId, true)
                .ifPresent(old -> {
                    old.setActive(false);
                    planRepository.save(old);
                });

        CreatorLivePlan plan = new CreatorLivePlan();

        plan.setId(UUID.randomUUID().toString());
        plan.setOwnerId(ownerId);
        plan.setTotalMinutes(1000);
        plan.setUsedMinutes(0);
        plan.setActive(true);

        return planRepository.save(plan);
    }

}