package com.mms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DefaulterResponse {
    private String studentId;
    private String studentName;
    private String className;
    private String guardianPhone;
    private Integer overdueAmount;
    private Integer monthsOverdue;
}

