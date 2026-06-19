// src/main/java/com/mms/controller/ProfileController.java
package com.mms.controller;

import com.mms.dto.request.ChangePasswordRequest;
import com.mms.dto.request.ProfileUpdateRequest;
import com.mms.dto.request.TwoFactorVerifyRequest;
import com.mms.dto.response.ProfileResponse;
import com.mms.dto.response.TwoFactorChallengeResponse;
import com.mms.entity.User;
import com.mms.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        log.debug("Getting current user profile");
        ProfileResponse profile = profileService.getCurrentProfile();
        return ResponseEntity.ok(profile);
    }

    @PatchMapping
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        log.info("Received profile update request");
        ProfileResponse updated = profileService.updateProfile(request);
        log.info("Profile updated successfully for user: {}", updated.getUsername());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Received password change request");
        profileService.changePassword(request);
        log.info("Password changed successfully");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<TwoFactorChallengeResponse> enableTwoFactor() {
        log.info("Received 2FA enable request");
        TwoFactorChallengeResponse response = profileService.enableTwoFactor();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<User> verifyTwoFactorEnable(@Valid @RequestBody TwoFactorVerifyRequest request) {
        log.info("Received 2FA verify request");
        User user = profileService.verifyTwoFactorEnable(request);
        log.info("2FA enabled successfully for user: {}", user.getUsername());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<User> disableTwoFactor() {
        log.info("Received 2FA disable request");
        User user = profileService.disableTwoFactor();
        log.info("2FA disabled successfully for user: {}", user.getUsername());
        return ResponseEntity.ok(user);
    }
}