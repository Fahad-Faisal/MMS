// src/main/java/com/mms/service/UserService.java
package com.mms.service;

import com.mms.dto.request.UserRequest;
import com.mms.dto.request.UserUpdateRequest;
import com.mms.entity.User;
import com.mms.enums.Role;
import com.mms.exception.ResourceNotFoundException;
import com.mms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Cacheable(value = "users", key = "{#search, #role, #page, #pageSize}")
    public Page<User> getUsers(String search, String role, int page, int pageSize) {
        log.debug("Fetching users from database - search: {}, role: {}, page: {}, pageSize: {}",
                search, role, page, pageSize);

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (search != null && !search.isEmpty()) {
            // Custom search implementation - you can add more sophisticated search
            return userRepository.findAll(pageable);
        }

        if (role != null && !role.isEmpty()) {
            // Filter by role
            try {
                Role roleEnum = Role.valueOf(role);
                // You would need a custom query for this
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role: {}", role);
            }
        }

        return userRepository.findAll(pageable);
    }

    @Cacheable(value = "user", key = "#id")
    public User getUser(String id) {
        log.debug("Fetching user from database - id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),  // Evict all user list caches
            @CacheEvict(value = "team", allEntries = true)    // Evict team cache
    })
    public User createUser(UserRequest request) {
        log.info("Creating new user with username: {}", request.getUsername());

        // Validate username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username '" + request.getUsername() + "' already exists");
        }

        // Validate email uniqueness
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email '" + request.getEmail() + "' already in use");
        }

        // Build user entity
        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(request.getRole())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .twoFactorEnabled(false)
                .build();

        // Save user
        User saved = userRepository.save(user);
        log.info("User created successfully with id: {}", saved.getId());

        // Log audit
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "CREATE_USER", "user", saved.getId());

        // Clear all user-related caches after transaction commit
        clearAllUserCaches();

        return saved;
    }

    @Transactional
    @Caching(
            put = {
                    @CachePut(value = "user", key = "#id")
            },
            evict = {
                    @CacheEvict(value = "users", allEntries = true),
                    @CacheEvict(value = "team", allEntries = true)
            }
    )
    public User updateUser(String id, UserUpdateRequest request) {
        log.info("Updating user with id: {}", id);

        User user = getUser(id);

        // Only update fields that are provided (not null)
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!request.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email '" + request.getEmail() + "' already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        User saved = userRepository.save(user);
        log.info("User updated successfully with id: {}", saved.getId());

        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "UPDATE_USER", "user", id);

        return saved;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user", key = "#userId"),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "team", allEntries = true)
    })
    public void resetPassword(String userId, String newPassword) {
        log.info("Resetting password for user: {}", userId);

        User user = getUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "RESET_PASSWORD", "user", userId);
        log.info("Password reset successfully for user: {}", userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user", key = "#id"),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "team", allEntries = true)
    })
    public void deleteUser(String id) {
        log.info("Deleting user with id: {}", id);

        User user = getUser(id);
        userRepository.deleteById(id);

        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "DELETE_USER", "user", id);
        log.info("User deleted successfully: {}", id);
    }

    // src/main/java/com/mms/service/UserService.java
// Fix the getTeamMembers method - add logging and debug

    @Cacheable(value = "team")
    public List<User> getTeamMembers() {
        try {
            log.info("Fetching team members for public display");

            // Get all users
            List<User> allUsers = userRepository.findAll();
            log.info("Total users in database: {}", allUsers.size());

            // Filter active users
            List<User> activeUsers = allUsers.stream()
                    .filter(User::isActive)
                    .collect(Collectors.toList());
            log.info("Active users: {}", activeUsers.size());

            // Optionally filter out ADMINISTRATOR for public view
            List<User> teamMembers = activeUsers.stream()
                    .filter(u -> u.getRole() != Role.ADMINISTRATOR)
                    .collect(Collectors.toList());
            log.info("Team members after role filter: {}", teamMembers.size());

            // Log each team member for debugging
            for (User user : teamMembers) {
                log.info("Team member: {} - Role: {} - Active: {}",
                        user.getFullName(), user.getRole(), user.isActive());
            }

            return teamMembers;

        } catch (Exception e) {
            log.error("Error fetching team members", e);
            return List.of();
        }
    }

    // Manual cache clearing method
    public void clearAllUserCaches() {
        // This method can be called programmatically if needed
        log.debug("Manually clearing all user caches");
        // The @CacheEvict annotations should handle this automatically
    }

    private String getCurrentUserId() {
        // Implementation to get current user ID from SecurityContext
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Could not get current user ID: {}", e.getMessage());
        }
        return "system";
    }

    private String getCurrentUserName() {
        // Implementation to get current user name from SecurityContext
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                // Try to get name from principal
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                }
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Could not get current user name: {}", e.getMessage());
        }
        return "system";
    }


}