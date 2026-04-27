package com.fanzzi.backend.appeal.repository;

import com.fanzzi.backend.appeal.enums.AppealStatus;
import com.fanzzi.backend.appeal.model.BanAppeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BanAppealRepository
        extends MongoRepository<BanAppeal, String> {

    List<BanAppeal> findByUserId(String userId);

    Page<BanAppeal> findByStatus(AppealStatus status, Pageable pageable);

    boolean existsByUserIdAndStatus(String userId, AppealStatus status);

    long countByStatus(AppealStatus status);
}
