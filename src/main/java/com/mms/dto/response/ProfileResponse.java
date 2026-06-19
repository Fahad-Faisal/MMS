// src/main/java/com/mms/dto/response/ProfileResponse.java
package com.mms.dto.response;

import com.mms.entity.User;
import com.mms.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String photoUrl;
    private String bio;
    private boolean twoFactorEnabled;
    private boolean active;
    private String createdAt;

    public static ProfileResponse fromUser(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .photoUrl(user.getPhotoUrl())
                .bio(user.getBio())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .active(user.isActive())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}