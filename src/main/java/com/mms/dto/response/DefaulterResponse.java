// src/main/java/com/mms/dto/response/DefaulterResponse.java
package com.mms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DefaulterResponse {
    private String studentId;
    private String studentName;
    private String rollNo;  // NEW
    private String className;
    private String classroomName;  // NEW
    private String guardianPhone;
    private Integer overdueAmount;
    private Integer monthsOverdue;
}