//package com.fanzzi.backend.auth.session.service;
//
//import com.fanzzi.backend.auth.session.dto.UserSessionDTO;
//import com.fanzzi.backend.user.model.User;
//import com.fanzzi.backend.user.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.CachePut;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Service;
//
//
//@Service
//@RequiredArgsConstructor
//public class UserSessionService {
//
//    private final UserRepository userRepository;
//
//    // Load from Redis cache
//    @Cacheable(value = "user_sessions", key = "#userId")
//    public UserSessionDTO getSession(String userId) {
//
//        return userRepository.findById(userId)
//                .map(user -> new UserSessionDTO(
//                        user.getId(),
//                        user.isActive(),
//                        user.isBanned(),
//                        user.isDeleted()
//                ))
//                .orElse(null);
//    }
//
//    // Save Session at Login
//    @CachePut(value = "user_sessions", key = "#user.id")
//    public UserSessionDTO saveSession(User user) {
//
//        return new UserSessionDTO(
//                user.getId(),
//                user.isActive(),
//                user.isBanned(),
//                user.isDeleted()
//        );
//    }
//
//    // Remove Session at Logout
//    @CacheEvict(value = "user_sessions", key = "#userId")
//    public void clearSession(String userId) {
//        // only cache eviction
//    }
//
////
//}
