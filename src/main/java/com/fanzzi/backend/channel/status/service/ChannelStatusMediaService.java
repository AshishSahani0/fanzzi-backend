package com.fanzzi.backend.channel.status.service;

import com.fanzzi.backend.media.gateway.status.channel.ChannelStatusMediaGateway;
import com.fanzzi.backend.media.status.ChannelStatusStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelStatusMediaService {

    private final ChannelStatusStorageService storage;
    private final ChannelStatusMediaGateway gateway;
    private final StringRedisTemplate redisTemplate;

    private static final Duration URL_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration NEGATIVE_CACHE_TTL = Duration.ofMinutes(2);

    private static final String CACHE_PREFIX = "status:media:url:";

    // 🔥 REQUEST LEVEL CACHE (VERY IMPORTANT)
    private final ThreadLocal<Map<String, String>> requestCache =
            ThreadLocal.withInitial(HashMap::new);



    public Map<String, String> createUploadUrl(
            String channelId,
            String fileName,
            long fileSize
    ) {
        return storage.createChannelStatusUploadUrl(
                channelId,
                fileName,
                fileSize
        );
    }

    // =====================================================
    // 🗑 DELETE MEDIA
    // =====================================================

    public void delete(String key) {
        if (key == null || key.isBlank()) return;

        try {
            gateway.deleteChannelStatusMedia(key);
        } catch (Exception e) {
            log.warn("Media delete failed key={}", key, e);
        }

        try {
            redisTemplate.delete(CACHE_PREFIX + key);
        } catch (Exception e) {
            log.warn("Cache delete failed key={}", key, e);
        }
    }

    // =====================================================
    // 🌍 SINGLE RESOLVE (OPTIMIZED)
    // =====================================================

    public String resolvePublicUrl(String key) {
        if (key == null) return null;

        Map<String, String> local = requestCache.get();

        // 🔥 CHECK REQUEST CACHE FIRST
        if (local.containsKey(key)) {
            return local.get(key);
        }

        String value = resolvePublicUrls(List.of(key)).get(key);

        local.put(key, value);
        return value;
    }

    // =====================================================
    // ⚡ ULTRA BULK RESOLVE (PRODUCTION LEVEL)
    // =====================================================

    public Map<String, String> resolvePublicUrls(List<String> keys) {

        if (keys == null || keys.isEmpty()) return Map.of();

        Map<String, String> result = new HashMap<>();
        Map<String, String> cacheMiss = new HashMap<>();

        Map<String, String> localCache = requestCache.get();

        // ================= LOCAL CACHE =================
        for (String key : keys) {
            if (localCache.containsKey(key)) {
                result.put(key, localCache.get(key));
            } else {
                cacheMiss.put(key, key);
            }
        }

        if (cacheMiss.isEmpty()) return result;

        // ================= REDIS PIPELINE =================
        List<String> cacheKeys = cacheMiss.keySet().stream()
                .map(k -> CACHE_PREFIX + k)
                .toList();

        List<Object> cachedValues = pipelineGet(cacheKeys);

        List<String> stillMiss = new ArrayList<>();

        int i = 0;
        for (String key : cacheMiss.keySet()) {

            Object cached = cachedValues.get(i++);

            if (cached != null) {
                String value = cached.toString();

                if (!"NULL".equals(value)) {
                    result.put(key, value);
                    localCache.put(key, value);
                }
            } else {
                stillMiss.add(key);
            }
        }

        // ================= FETCH REMAINING (BATCHED) =================
        if (!stillMiss.isEmpty()) {

            Map<String, String> fetched = fetchBatch(stillMiss);

            for (String key : stillMiss) {

                String url = fetched.get(key);

                if (url != null) {
                    result.put(key, url);
                    localCache.put(key, url);
                    cachePut(key, url);
                } else {
                    cacheNull(CACHE_PREFIX + key);
                }
            }
        }

        return result;
    }

    // =====================================================
    // 🔥 REDIS PIPELINE GET
    // =====================================================

    private List<Object> pipelineGet(List<String> keys) {
        try {
            return redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                for (String key : keys) {
                    connection.stringCommands().get(key.getBytes());
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("Pipeline get failed", e);
            return Collections.nCopies(keys.size(), null);
        }
    }

    // =====================================================
    // 🚀 BATCH FETCH (MAJOR UPGRADE)
    // =====================================================

    private Map<String, String> fetchBatch(List<String> keys) {

        Map<String, String> result = new HashMap<>();

        for (String key : keys) {

            String url = fetchWithFallback(key);

            if (url != null) {
                result.put(key, url);
            }
        }

        return result;
    }

    // =====================================================
    // 🌍 CDN FALLBACK
    // =====================================================

    private String fetchWithFallback(String key) {

        try {
            return gateway.getChannelStatusUrl(key);
        } catch (Exception e1) {

            log.warn("Primary CDN failed key={}", key);

            try {
                return storage.getChannelStatusUrl(key);
            } catch (Exception e2) {
                log.error("Fallback CDN failed key={}", key, e2);
                return null;
            }
        }
    }

    // =====================================================
    // 🔥 PRE-WARM CACHE (ASYNC SAFE)
    // =====================================================

    public void preWarmUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) return;

        try {
            resolvePublicUrls(keys);
        } catch (Exception e) {
            log.warn("Prewarm failed", e);
        }
    }

    // =====================================================
    // 🔥 CACHE HELPERS
    // =====================================================

    private void cachePut(String key, String url) {
        try {
            redisTemplate.opsForValue()
                    .set(CACHE_PREFIX + key, url, URL_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Cache set failed key={}", key, e);
        }
    }

    private void cacheNull(String cacheKey) {
        try {
            redisTemplate.opsForValue()
                    .set(cacheKey, "NULL", NEGATIVE_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Negative cache failed key={}", cacheKey, e);
        }
    }
}