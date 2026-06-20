// src/main/java/com/mms/dto/response/ForgotPasswordResponse.java
package com.mms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordResponse {
    private String challengeId;
    private String emailHint;
}