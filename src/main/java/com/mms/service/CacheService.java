// src/main/java/com/mms/service/CacheService.java
package com.mms.service;

import com.mms.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    private final CacheManager cacheManager;

    private static final String TEAM_CACHE = "team";
    private static final String USER_CACHE = "user";
    private static final String USERS_CACHE = "users";

    /**
     * Clear the team cache
     */
    public void clearTeamCache() {
        try {
            Cache cache = cacheManager.getCache(TEAM_CACHE);
            if (cache != null) {
                cache.clear();
                log.info("✅ Team cache cleared successfully");
            } else {
                log.warn("Team cache not found");
            }
        } catch (Exception e) {
            log.error("Failed to clear team cache", e);
        }
    }

    /**
     * Clear the user cache for a specific user
     */
    public void clearUserCache(String userId) {
        try {
            Cache cache = cacheManager.getCache(USER_CACHE);
            if (cache != null) {
                cache.evict(userId);
                log.info("✅ User cache cleared for ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to clear user cache for ID: {}", userId, e);
        }
    }

    /**
     * Clear all users cache
     */
    public void clearAllUsersCache() {
        try {
            Cache cache = cacheManager.getCache(USERS_CACHE);
            if (cache != null) {
                cache.clear();
                log.info("✅ All users cache cleared");
            }
        } catch (Exception e) {
            log.error("Failed to clear all users cache", e);
        }
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                try {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                        log.debug("Cleared cache: {}", cacheName);
                    }
                } catch (Exception e) {
                    log.warn("Could not clear cache: {}", cacheName, e);
                }
            });
            log.info("✅ All caches cleared successfully");
        } catch (Exception e) {
            log.error("Failed to clear all caches", e);
        }
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        try {
            StringBuilder stats = new StringBuilder();
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    stats.append(cacheName).append(": ").append(cache.getClass().getSimpleName()).append("\n");
                }
            });
            return stats.toString();
        } catch (Exception e) {
            return "Failed to get cache stats: " + e.getMessage();
        }
    }
}