package com.fanzzi.backend.auth.repository;

import com.fanzzi.backend.auth.model.AuthUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AuthUserRepository
        extends MongoRepository<AuthUser, String> {

    Optional<AuthUser> findByPhone(String phone);
}