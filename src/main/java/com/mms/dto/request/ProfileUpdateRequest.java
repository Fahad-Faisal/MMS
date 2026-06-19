// src/main/java/com/mms/dto/request/ProfileUpdateRequest.java
package com.mms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(min = 2, max = 80, message = "Full name must be between 2 and 80 characters")
    private String fullName;

    @Size(max = 254, message = "Email too long")
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20, message = "Phone number too long")
    private String phone;

    @Size(max = 2000, message = "Bio too long")
    private String bio;

    @Size(max = 2000, message = "Photo URL too long")
    private String photoUrl;
}