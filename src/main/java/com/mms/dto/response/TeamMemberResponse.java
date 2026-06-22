// src/main/java/com/mms/dto/response/TeamMemberResponse.java
package com.mms.dto.response;

import com.mms.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamMemberResponse {
    private String id;
    private String fullName;
    private String role;
    private String photoUrl;
    private String bio;

    public static TeamMemberResponse fromUser(User user) {
        return TeamMemberResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .photoUrl(user.getPhotoUrl())
                .bio(user.getBio())
                .build();
    }
}