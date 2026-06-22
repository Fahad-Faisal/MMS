// src/main/java/com/mms/controller/TeamController.java
package com.mms.controller;

import com.mms.entity.User;
import com.mms.enums.Role;
import com.mms.repository.UserRepository;
import com.mms.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
@Slf4j
public class TeamController {
    private final UserRepository userRepository;
    private final CacheService cacheService;

    @GetMapping
    public ResponseEntity<?> getTeam() {
        try {
            log.info("Getting team members for landing page");

            // Get all users from database directly (bypass cache)
            List<User> allUsers = userRepository.findAll();
            log.info("Total users in database: {}", allUsers.size());

            if (allUsers.isEmpty()) {
                log.warn("No users found in database!");
                return ResponseEntity.ok(List.of());
            }

            // Log all users for debugging
            for (User user : allUsers) {
                log.info("User found: username={}, role={}, active={}",
                        user.getUsername(), user.getRole(), user.isActive());
            }

            // Filter: active users, exclude ADMINISTRATOR
            List<User> teamMembers = allUsers.stream()
                    .filter(User::isActive)
                    .filter(user -> user.getRole() != Role.ADMINISTRATOR)
                    .collect(Collectors.toList());

            log.info("After filtering: {} team members", teamMembers.size());

            if (teamMembers.isEmpty()) {
                log.info("No team members after filtering.");
                return ResponseEntity.ok(List.of());
            }

            // Map to response
            List<Map<String, String>> response = teamMembers.stream()
                    .map(user -> Map.of(
                            "id", user.getId(),
                            "fullName", user.getFullName(),
                            "role", user.getRole().name(),
                            "photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl() : "",
                            "bio", user.getBio() != null ? user.getBio() : ""
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching team members", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Endpoint to clear team cache
     * Useful for debugging
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, String>> clearTeamCache() {
        try {
            cacheService.clearTeamCache();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Team cache cleared successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to clear team cache", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to clear cache: " + e.getMessage()
                    ));
        }
    }

    /**
     * Debug endpoint to check cache status
     */
    @GetMapping("/cache-status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        return ResponseEntity.ok(Map.of(
                "cacheStats", cacheService.getCacheStats(),
                "totalUsers", userRepository.count(),
                "activeUsers", userRepository.findAll().stream()
                        .filter(User::isActive)
                        .count()
        ));
    }
}