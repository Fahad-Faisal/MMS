// src/main/java/com/mms/dto/response/AuthResponse.java
package com.mms.dto.response;

import com.mms.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private String id;
        private String username;
        private String fullName;
        private String role;
        private String email;
        private String phone;
        private String photoUrl;
        private String bio;
        private Boolean twoFactorEnabled;
    }

    public static AuthResponse fromUser(User user, String token, String refreshToken) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .user(UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .photoUrl(user.getPhotoUrl())
                        .bio(user.getBio())
                        .twoFactorEnabled(user.isTwoFactorEnabled())
                        .build())
                .build();
    }
}