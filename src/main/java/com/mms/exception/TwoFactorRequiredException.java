package com.mms.exception;

import lombok.Getter;

@Getter
public class TwoFactorRequiredException extends RuntimeException {
    private final String challengeId;
    private final String emailHint;

    public TwoFactorRequiredException(String challengeId, String emailHint) {
        super("Two-factor authentication required");
        this.challengeId = challengeId;
        this.emailHint = emailHint;
    }
}