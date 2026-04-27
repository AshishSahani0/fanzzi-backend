package com.fanzzi.backend.user.listener;

import com.fanzzi.backend.auth.events.UserCreatedEvent;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserProfileInitializer {

    private final UserRepository repo;

    @EventListener
    public void handle(UserCreatedEvent event) {

        try {

            User user = new User();

            user.setId(event.userId());
            user.setPhone(event.phone());
            user.setCountryCode(event.countryCode());
            user.setCreatedAt(Instant.now());
            user.setActive(true);

            repo.insert(user); // insert instead of save

        } catch (Exception ignored) {

            // user already exists → ignore safely

        }

    }
}