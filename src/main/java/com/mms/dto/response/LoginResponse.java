// src/main/java/com/mms/dto/response/LoginResponse.java
package com.mms.dto.response;

import com.mms.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private UserDto user;
    private Boolean twoFactorRequired;
    private String challengeId;
    private String emailHint;

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
        private Boolean active;

        public static UserDto fromUser(User user) {
            return UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .photoUrl(user.getPhotoUrl())
                    .bio(user.getBio())
                    .twoFactorEnabled(user.isTwoFactorEnabled())
                    .active(user.isActive())
                    .build();
        }
    }
}