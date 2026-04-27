package com.fanzzi.backend.common.infrastructure.rate_limit;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public void checkLimit(String key, int maxRequests, Duration window) {

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }

        if (count != null && count > maxRequests) {
            throw new ApiException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "Too many requests. Try again later."
            );
        }
    }
}