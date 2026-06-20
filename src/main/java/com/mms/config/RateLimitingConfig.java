// src/main/java/com/mms/config/RateLimitingConfig.java
package com.mms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    private final Map<String, Map<String, Integer>> forgotPasswordAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> forgotPasswordResetTime = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_USERNAME = 3;
    private static final int MAX_REQUESTS_PER_IP = 10;
    private static final int TIME_WINDOW_MINUTES = 15;
    private static final int IP_TIME_WINDOW_HOURS = 1;

    @Bean
    public RateLimitingService rateLimitingService() {
        return new RateLimitingService();
    }

    public class RateLimitingService {

        public boolean isForgotPasswordAllowed(String username, String clientIp) {
            // Check username-based limit
            String usernameKey = "username:" + username;
            String ipKey = "ip:" + clientIp;

            // Clean old entries
            cleanOldEntries();

            // Check username limit
            Map<String, Integer> usernameAttempts = forgotPasswordAttempts.computeIfAbsent(
                    usernameKey, k -> new ConcurrentHashMap<>()
            );
            int userAttempts = usernameAttempts.values().stream().mapToInt(Integer::intValue).sum();

            if (userAttempts >= MAX_REQUESTS_PER_USERNAME) {
                return false;
            }

            // Check IP limit
            Map<String, Integer> ipAttempts = forgotPasswordAttempts.computeIfAbsent(
                    ipKey, k -> new ConcurrentHashMap<>()
            );
            int ipAttemptCount = ipAttempts.values().stream().mapToInt(Integer::intValue).sum();

            if (ipAttemptCount >= MAX_REQUESTS_PER_IP) {
                return false;
            }

            // Record attempt
            usernameAttempts.merge(LocalDateTime.now().toString(), 1, Integer::sum);
            ipAttempts.merge(LocalDateTime.now().toString(), 1, Integer::sum);

            return true;
        }

        private void cleanOldEntries() {
            // Clean entries older than time window
            // Implementation depends on your cleanup strategy
        }
    }
}