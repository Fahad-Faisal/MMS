// src/main/java/com/mms/service/AuthService.java
package com.mms.service;

import com.mms.dto.request.LoginRequest;
import com.mms.dto.request.TwoFactorVerifyRequest;
import com.mms.dto.response.AuthResponse;
import com.mms.dto.response.LoginResponse;
import com.mms.entity.User;
import com.mms.entity.TwoFactorChallenge;
import com.mms.enums.Role;
import com.mms.exception.InvalidCredentialsException;
import com.mms.exception.ResourceNotFoundException;
import com.mms.exception.UnauthorizedException;
import com.mms.repository.TwoFactorChallengeRepository;
import com.mms.repository.UserRepository;
import com.mms.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final TwoFactorChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final RestTemplate restTemplate;

    @Value("${turnstile.secret-key}")
    private String turnstileSecretKey;

    @Value("${turnstile.required:true}")
    private boolean turnstileRequired;

    @Value("${app.url}")
    private String appUrl;

    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    /**
     * Authenticate user with username and password
     * Supports 2FA if enabled
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        // Verify Turnstile CAPTCHA
        if (turnstileRequired) {
            verifyTurnstile(request.getTurnstileToken());
        }

        // Find user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getUsername());
                    return new InvalidCredentialsException("Invalid username or password");
                });

        // Check if user is active
        if (!user.isActive()) {
            log.warn("Login failed - user is inactive: {}", request.getUsername());
            throw new InvalidCredentialsException("Account is disabled. Please contact administrator.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed - invalid password for user: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        log.info("Password verified for user: {}", user.getUsername());

        // Check if 2FA is enabled
        if (user.isTwoFactorEnabled() && user.getEmail() != null && !user.getEmail().isEmpty()) {
            log.info("2FA is enabled for user: {}, initiating 2FA flow", user.getUsername());
            return initiateTwoFactorChallenge(user);
        }

        // No 2FA - return token directly
        log.info("Login successful for user: {} (no 2FA)", user.getUsername());
        return createLoginResponse(user);
    }

    /**
     * Verify 2FA code and complete login
     */
    // src/main/java/com/mms/service/AuthService.java - Update verifyTwoFactor method

    @Transactional
    public LoginResponse verifyTwoFactor(TwoFactorVerifyRequest request) {
        try {
            log.info("Verifying 2FA code for challenge: {}", request.getChallengeId());

            // Validate request
            if (request.getChallengeId() == null || request.getChallengeId().isEmpty()) {
                throw new InvalidCredentialsException("Challenge ID is required");
            }
            if (request.getCode() == null || request.getCode().isEmpty()) {
                throw new InvalidCredentialsException("Verification code is required");
            }
            if (request.getCode().length() != 6) {
                throw new InvalidCredentialsException("Verification code must be 6 digits");
            }

            // Find challenge
            TwoFactorChallenge challenge = challengeRepository.findById(request.getChallengeId())
                    .orElseThrow(() -> {
                        log.warn("2FA verification failed - challenge not found: {}", request.getChallengeId());
                        return new InvalidCredentialsException("Invalid or expired challenge");
                    });

            // Check if challenge is expired
            if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
                challengeRepository.delete(challenge);
                log.warn("2FA verification failed - challenge expired: {}", request.getChallengeId());
                throw new InvalidCredentialsException("Code has expired. Please sign in again.");
            }

            // Check attempts
            if (challenge.getAttempts() >= MAX_ATTEMPTS) {
                challengeRepository.delete(challenge);
                log.warn("2FA verification failed - too many attempts for challenge: {}", request.getChallengeId());
                throw new InvalidCredentialsException("Too many failed attempts. Please try again.");
            }

            // Verify code using BCrypt matches
            boolean codeMatches = passwordEncoder.matches(request.getCode(), challenge.getCodeHash());

            if (!codeMatches) {
                challenge.setAttempts(challenge.getAttempts() + 1);
                challengeRepository.save(challenge);
                int remainingAttempts = MAX_ATTEMPTS - challenge.getAttempts();
                log.warn("2FA verification failed - incorrect code for challenge: {}, attempts: {}/{}",
                        request.getChallengeId(), challenge.getAttempts(), MAX_ATTEMPTS);
                throw new InvalidCredentialsException(String.format(
                        "Incorrect verification code. %d attempt(s) remaining.",
                        remainingAttempts
                ));
            }

            // Get user
            User user = userRepository.findById(challenge.getUserId())
                    .orElseThrow(() -> {
                        log.error("2FA verification failed - user not found: {}", challenge.getUserId());
                        return new ResourceNotFoundException("User not found");
                    });

            // Delete challenge
            challengeRepository.delete(challenge);

            log.info("2FA verification successful for user: {}", user.getUsername());
            return createLoginResponse(user);

        } catch (InvalidCredentialsException | ResourceNotFoundException e) {
            // Re-throw these exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during 2FA verification: {}", e.getMessage(), e);
            throw new InvalidCredentialsException("An unexpected error occurred. Please try again.");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new InvalidCredentialsException("Refresh token is required");
        }

        try {
            String userId = jwtService.extractUserId(refreshToken);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("Refresh token failed - user not found: {}", userId);
                        return new ResourceNotFoundException("User not found");
                    });

            if (!jwtService.isTokenValid(refreshToken, userId)) {
                log.warn("Refresh token failed - invalid token for user: {}", userId);
                throw new InvalidCredentialsException("Invalid refresh token");
            }

            log.info("Token refreshed successfully for user: {}", user.getUsername());
            return createLoginResponse(user);
        } catch (Exception e) {
            log.error("Refresh token failed", e);
            throw new InvalidCredentialsException("Invalid refresh token");
        }
    }

    /**
     * Get current authenticated user
     */
    public User getCurrentUser() {
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

    /**
     * Get current authenticated user profile
     */
    public AuthResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return AuthResponse.fromUser(user, null, null);
    }

    /**
     * Logout - clear any session data
     */
    @CacheEvict(value = "user", key = "#userId")
    public void logout(String userId) {
        log.info("Logging out user: {}", userId);
        // In a stateless JWT system, we just clear the client-side token
        // Optionally, we could blacklist the token here
    }

    /**
     * Initiate 2FA challenge for a user
     */
    private LoginResponse initiateTwoFactorChallenge(User user) {
        String code = generateVerificationCode();
        String challengeId = UUID.randomUUID().toString();

        TwoFactorChallenge challenge = TwoFactorChallenge.builder()
                .id(challengeId)
                .userId(user.getId())
                .codeHash(passwordEncoder.encode(code))
                .purpose("LOGIN")
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .attempts(0)
                .build();

        challengeRepository.save(challenge);

        // Send 2FA code via email
        try {
            emailService.sendTwoFactorCode(user.getEmail(), user.getFullName(), code, "LOGIN");
            log.info("2FA code sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send 2FA code to: {}", user.getEmail(), e);
            // Don't throw - the user will see the challenge but won't receive the email
            // This is a temporary issue that should be fixed
        }

        return LoginResponse.builder()
                .twoFactorRequired(true)
                .challengeId(challengeId)
                .emailHint(maskEmail(user.getEmail()))
                .build();
    }

    /**
     * Create login response with JWT tokens
     */
    private LoginResponse createLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Log audit
        auditService.logAudit(user.getId(), user.getFullName(), "LOGIN", "user", user.getId());

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .user(LoginResponse.UserDto.fromUser(user))
                .twoFactorRequired(false)
                .build();
    }

    /**
     * Verify Turnstile CAPTCHA token
     */
    private void verifyTurnstile(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Turnstile verification failed - token is null or empty");
            throw new InvalidCredentialsException("Security check required");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = String.format("secret=%s&response=%s", turnstileSecretKey, token);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    TURNSTILE_VERIFY_URL,
                    entity,
                    Map.class
            );

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                log.warn("Turnstile verification failed - invalid response");
                throw new InvalidCredentialsException("Invalid security check");
            }

            log.debug("Turnstile verification successful");
        } catch (Exception e) {
            log.error("Turnstile verification error", e);
            throw new InvalidCredentialsException("Security check failed");
        }
    }

    /**
     * Verify 2FA code against stored hash
     */
    private boolean verifyCode(String rawCode, String hashedCode) {
        try {
            return passwordEncoder.matches(rawCode, hashedCode);
        } catch (Exception e) {
            log.error("Code verification failed", e);
            return false;
        }
    }

    /**
     * Generate a random 6-digit verification code
     */
    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    /**
     * Mask email for security (e.g., a***@example.com)
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        String[] parts = email.split("@");
        if (parts.length != 2) return email;
        String username = parts[0];
        if (username.length() <= 1) return email;
        return username.charAt(0) + "***@" + parts[1];
    }
}