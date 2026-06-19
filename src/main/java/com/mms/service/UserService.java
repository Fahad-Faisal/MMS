// src/main/java/com/mms/service/UserService.java
package com.mms.service;

import com.mms.dto.request.UserRequest;
import com.mms.dto.request.UserUpdateRequest;
import com.mms.entity.User;
import com.mms.enums.Role;
import com.mms.exception.ResourceNotFoundException;
import com.mms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Cacheable(value = "users", key = "{#search, #role, #page, #pageSize}")
    public Page<User> getUsers(String search, String role, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        if (search != null || role != null) {
            // You can add custom query here
            return userRepository.findAll(pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Cacheable(value = "user", key = "#id")
    public User getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional

    public User createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(request.getRole())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .twoFactorEnabled(false)
                .build();

        User saved = userRepository.save(user);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "CREATE_USER", "user", saved.getId());
        return saved;
    }

    @Transactional

    public User updateUser(String id, UserUpdateRequest request) {
        User user = getUser(id);

        // Only update fields that are provided (not null)
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use");
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

        // Handle active flag - only update if explicitly provided
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        User saved = userRepository.save(user);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "UPDATE_USER", "user", id);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "user", key = "#userId")
    public void resetPassword(String userId, String newPassword) {
        User user = getUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "RESET_PASSWORD", "user", userId);
    }

    @Transactional

    public void deleteUser(String id) {
        userRepository.deleteById(id);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "DELETE_USER", "user", id);
    }

    @Cacheable(value = "team")
    public List<User> getTeamMembers() {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> u.getRole() != Role.ADMINISTRATOR)
                .toList();
    }

    private String getCurrentUserId() {
        // Implementation will get from SecurityContext
        return "system";
    }

    private String getCurrentUserName() {
        // Implementation will get from SecurityContext
        return "system";
    }
}