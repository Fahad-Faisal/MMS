package com.mms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorChallengeResponse {
    private String challengeId;
    private String emailHint;
}
