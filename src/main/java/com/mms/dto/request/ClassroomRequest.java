// src/main/java/com/mms/dto/request/ClassroomRequest.java
package com.mms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ClassroomRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String className;

    private String teacherId;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 500, message = "Capacity cannot exceed 500")
    private Integer capacity;  // NEW

    private List<String> studentIds;
}