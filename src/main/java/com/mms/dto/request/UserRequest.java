// src/main/java/com/mms/dto/request/UserRequest.java
package com.mms.dto.request;

import com.mms.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
    private String username;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 80, message = "Full name must be between 2 and 80 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email too long")
    private String email;

    @NotNull(message = "Role is required")  // Use @NotNull for enums
    private Role role;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    private String password;
}