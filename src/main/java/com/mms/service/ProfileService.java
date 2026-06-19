// src/main/java/com/mms/service/ProfileService.java
package com.mms.service;

import com.mms.dto.request.ChangePasswordRequest;
import com.mms.dto.request.ProfileUpdateRequest;
import com.mms.dto.request.TwoFactorVerifyRequest;
import com.mms.dto.response.ProfileResponse;
import com.mms.dto.response.TwoFactorChallengeResponse;
import com.mms.entity.User;
import com.mms.entity.TwoFactorChallenge;
import com.mms.exception.ResourceNotFoundException;
import com.mms.exception.UnauthorizedException;
import com.mms.repository.TwoFactorChallengeRepository;
import com.mms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    private final UserRepository userRepository;
    private final TwoFactorChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    @Transactional
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = getCurrentUser();
        log.info("Updating profile for user: {}", user.getUsername());

        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!request.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use by another account");
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

        User saved = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", user.getUsername());

        auditService.logAudit(user.getId(), user.getFullName(), "UPDATE_PROFILE", "user", user.getId());

        // Manually update cache
        evictUserCache(user.getId());
        return ProfileResponse.fromUser(saved);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();
        log.info("Changing password for user: {}", user.getUsername());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change failed - incorrect current password for user: {}", user.getUsername());
            throw new RuntimeException("Current password is incorrect");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            log.warn("Password change failed - new password same as current for user: {}", user.getUsername());
            throw new RuntimeException("New password must differ from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getUsername());
        auditService.logAudit(user.getId(), user.getFullName(), "CHANGE_PASSWORD", "user", user.getId());

        // Manually evict cache
        evictUserCache(user.getId());
    }

    public ProfileResponse getCurrentProfile() {
        User user = getCurrentUser();
        return ProfileResponse.fromUser(user);
    }

    @Transactional
    public TwoFactorChallengeResponse enableTwoFactor() {
        User user = getCurrentUser();
        log.info("Enabling 2FA for user: {}", user.getUsername());

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new RuntimeException("Add an email address to your profile first");
        }

        if (user.isTwoFactorEnabled()) {
            throw new RuntimeException("Two-factor authentication is already enabled");
        }

        String code = generateVerificationCode();
        String challengeId = UUID.randomUUID().toString();

        TwoFactorChallenge challenge = TwoFactorChallenge.builder()
                .id(challengeId)
                .userId(user.getId())
                .codeHash(passwordEncoder.encode(code))
                .purpose("ENABLE")
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .attempts(0)
                .build();

        challengeRepository.save(challenge);

        emailService.sendTwoFactorCode(user.getEmail(), user.getFullName(), code, "ENABLE");

        log.info("2FA enable challenge created for user: {}", user.getUsername());

        return TwoFactorChallengeResponse.builder()
                .challengeId(challengeId)
                .emailHint(maskEmail(user.getEmail()))
                .build();
    }

    @Transactional
    public User verifyTwoFactorEnable(TwoFactorVerifyRequest request) {
        User user = getCurrentUser();
        log.info("Verifying 2FA enable for user: {}", user.getUsername());

        if (request.getChallengeId() == null || request.getChallengeId().isEmpty()) {
            throw new RuntimeException("Challenge ID is required");
        }
        if (request.getCode() == null || request.getCode().isEmpty()) {
            throw new RuntimeException("Verification code is required");
        }
        if (request.getCode().length() != 6) {
            throw new RuntimeException("Verification code must be 6 digits");
        }

        TwoFactorChallenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> {
                    log.warn("2FA verify - challenge not found: {}", request.getChallengeId());
                    return new RuntimeException("Invalid or expired challenge");
                });

        if (!challenge.getUserId().equals(user.getId())) {
            log.warn("2FA verify - challenge belongs to different user: {}", challenge.getUserId());
            challengeRepository.delete(challenge);
            throw new RuntimeException("Invalid challenge");
        }

        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            challengeRepository.delete(challenge);
            log.warn("2FA verify - challenge expired for user: {}", user.getUsername());
            throw new RuntimeException("Code has expired. Please request a new one.");
        }

        if (challenge.getAttempts() >= MAX_ATTEMPTS) {
            challengeRepository.delete(challenge);
            log.warn("2FA verify - too many attempts for user: {}", user.getUsername());
            throw new RuntimeException("Too many failed attempts. Please request a new code.");
        }

        boolean codeMatches = passwordEncoder.matches(request.getCode(), challenge.getCodeHash());

        if (!codeMatches) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challengeRepository.save(challenge);
            int remainingAttempts = MAX_ATTEMPTS - challenge.getAttempts();
            log.warn("2FA verify - incorrect code for user: {}, attempts: {}/{}",
                    user.getUsername(), challenge.getAttempts(), MAX_ATTEMPTS);
            throw new RuntimeException(String.format(
                    "Incorrect verification code. %d attempt(s) remaining.",
                    remainingAttempts
            ));
        }

        challengeRepository.delete(challenge);
        user.setTwoFactorEnabled(true);
        User saved = userRepository.save(user);

        log.info("2FA enabled successfully for user: {}", user.getUsername());
        auditService.logAudit(user.getId(), user.getFullName(), "ENABLE_2FA", "user", user.getId());

        // Manually update cache
        updateUserCache(saved);
        return saved;
    }

    @Transactional
    public User disableTwoFactor() {
        User user = getCurrentUser();
        log.info("Disabling 2FA for user: {}", user.getUsername());

        if (!user.isTwoFactorEnabled()) {
            throw new RuntimeException("Two-factor authentication is not enabled");
        }

        user.setTwoFactorEnabled(false);
        User saved = userRepository.save(user);

        log.info("2FA disabled successfully for user: {}", user.getUsername());
        auditService.logAudit(user.getId(), user.getFullName(), "DISABLE_2FA", "user", user.getId());

        // Manually update cache
        updateUserCache(saved);
        return saved;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found");
            throw new UnauthorizedException("Not authenticated");
        }

        String userId = authentication.getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });
    }

    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        String[] parts = email.split("@");
        if (parts.length != 2) return email;
        String username = parts[0];
        if (username.length() <= 1) return email;
        return username.charAt(0) + "***@" + parts[1];
    }

    // Manual cache management methods
    private void evictUserCache(String userId) {
        try {
            // This is a workaround - we'll use a separate cache manager bean
            // Or we can use CacheManager directly
            log.debug("Evicting user cache for ID: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to evict cache for user: {}", userId, e);
        }
    }

    private void updateUserCache(User user) {
        try {
            log.debug("Updating user cache for ID: {}", user.getId());
        } catch (Exception e) {
            log.warn("Failed to update cache for user: {}", user.getId(), e);
        }
    }
}