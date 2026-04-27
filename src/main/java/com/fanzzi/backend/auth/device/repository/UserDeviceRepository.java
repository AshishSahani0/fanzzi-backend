package com.fanzzi.backend.auth.device.repository;

import com.fanzzi.backend.auth.device.model.UserDevice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository
        extends MongoRepository<UserDevice, String> {

    Optional<UserDevice> findByUserIdAndDeviceId(
            String userId,
            String deviceId
    );

    List<UserDevice> findByUserId(String userId);

    void deleteByUserIdAndDeviceId(
            String userId,
            String deviceId
    );
}