// src/main/java/com/mms/dto/request/BulkClassUpdateRequest.java
package com.mms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkClassUpdateRequest {
    @NotEmpty(message = "At least one student must be selected")
    private List<String> studentIds;

    @NotBlank(message = "Target class is required")
    private String className;

    private String classroomId;
}