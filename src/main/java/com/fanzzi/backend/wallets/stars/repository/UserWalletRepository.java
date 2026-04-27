package com.fanzzi.backend.wallets.stars.repository;

import com.fanzzi.backend.wallets.stars.model.UserWallet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWalletRepository
        extends MongoRepository<UserWallet, String> {

    // Fetch wallet by userId
    Optional<UserWallet> findByUserId(String userId);

    // Faster existence check (useful before creation)
    boolean existsByUserId(String userId);

    // Delete wallet (admin use only)
    void deleteByUserId(String userId);
}