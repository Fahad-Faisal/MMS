// src/main/java/com/mms/dto/request/UserUpdateRequest.java
package com.mms.dto.request;

import com.mms.enums.Role;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Boolean active;  // Using Boolean wrapper to handle null
    private String photoUrl;
    private String bio;
}