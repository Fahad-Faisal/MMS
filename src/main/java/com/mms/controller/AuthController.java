// src/main/java/com/mms/controller/AuthController.java
package com.mms.controller;

import com.mms.dto.request.*;
import com.mms.dto.response.ApiError;
import com.mms.dto.response.AuthResponse;
import com.mms.dto.response.ForgotPasswordResponse;
import com.mms.dto.response.LoginResponse;
import com.mms.exception.InvalidCredentialsException;
import com.mms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.debug("Login request received for username: {}", request.getUsername());
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed for username: {}", request.getUsername(), e);
            return ResponseEntity.status(401)
                    .body(ApiError.builder()
                            .status(401)
                            .message(e.getMessage())
                            .code("AUTH_FAILED")
                            .build());
        }
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request) {
        try {
            log.debug("2FA verification request received");
            LoginResponse response = authService.verifyTwoFactor(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("2FA verification failed", e);
            return ResponseEntity.status(400)
                    .body(ApiError.builder()
                            .status(400)
                            .message(e.getMessage())
                            .code("2FA_FAILED")
                            .build());
        }
    }
    // src/main/java/com/mms/controller/AuthController.java
// Add these methods to the existing AuthController

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.debug("Forgot password request received for username: {}", request.getUsername());
            ForgotPasswordResponse response = authService.forgotPassword(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Forgot password failed for username: {}", request.getUsername(), e);
            return ResponseEntity.status(400)
                    .body(ApiError.builder()
                            .status(400)
                            .message(e.getMessage())
                            .code("FORGOT_PASSWORD_FAILED")
                            .build());
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetForgottenPassword(@Valid @RequestBody ForgotPasswordResetRequest request) {
        try {
            log.debug("Password reset request received");
            authService.resetForgottenPassword(request);
            return ResponseEntity.noContent().build();
        } catch (InvalidCredentialsException e) {
            log.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiError.builder()
                            .status(400)
                            .message(e.getMessage())
                            .code(e.getMessage().contains("attempt") ? "INVALID_CODE" : "TOO_MANY_ATTEMPTS")
                            .build());
        } catch (Exception e) {
            log.error("Password reset failed", e);
            return ResponseEntity.status(400)
                    .body(ApiError.builder()
                            .status(400)
                            .message(e.getMessage())
                            .code("RESET_FAILED")
                            .build());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.debug("Token refresh request received");
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(401)
                    .body(ApiError.builder()
                            .status(401)
                            .message(e.getMessage())
                            .code("REFRESH_FAILED")
                            .build());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            AuthResponse response = authService.getCurrentUserProfile();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return ResponseEntity.status(401)
                    .body(ApiError.builder()
                            .status(401)
                            .message(e.getMessage())
                            .code("UNAUTHORIZED")
                            .build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        try {
            authService.logout(authService.getCurrentUser().getId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.noContent().build();
        }
    }
}