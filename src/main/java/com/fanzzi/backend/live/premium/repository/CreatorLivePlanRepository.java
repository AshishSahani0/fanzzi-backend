package com.fanzzi.backend.live.premium.repository;

import com.fanzzi.backend.live.premium.model.CreatorLivePlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CreatorLivePlanRepository
        extends MongoRepository<CreatorLivePlan,String> {

    Optional<CreatorLivePlan> findByOwnerIdAndActive(String ownerId, boolean active);

}
