// src/main/java/com/mms/dto/request/ForgotPasswordRequest.java
package com.mms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Username is required")
    private String username;
}