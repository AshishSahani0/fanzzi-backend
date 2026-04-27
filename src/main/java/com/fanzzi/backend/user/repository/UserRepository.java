package com.fanzzi.backend.user.repository;

import com.fanzzi.backend.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByUserNameLower(String userNameLower);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByUserNameLower(String userNameLower);

    List<User> findByIdIn(List<String> ids);
}