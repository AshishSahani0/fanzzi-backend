package com.fanzzi.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsyncExecutor {

    @Async
    public void run(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            // log error (never break main flow)
            System.err.println("Async task failed: " + e.getMessage());
        }
    }
}