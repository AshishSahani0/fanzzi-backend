package com.fanzzi.backend.wallets.platform;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlatformWalletRepository
        extends MongoRepository<PlatformWallet, String> {
}