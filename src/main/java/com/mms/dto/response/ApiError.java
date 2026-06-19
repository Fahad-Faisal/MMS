package com.mms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ApiError {
    private int status;
    private String message;
    private String code;
    private Map<String, String> fieldErrors;
}
