package com.mms.dto.request;

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

    private List<String> studentIds;
}