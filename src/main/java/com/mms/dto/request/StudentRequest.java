// src/main/java/com/mms/dto/request/StudentRequest.java
package com.mms.dto.request;

import com.mms.enums.StudentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentRequest {
    @NotBlank(message = "Roll number is required")
    private String rollNo;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String fatherName;
    private String motherName;
    private LocalDate dateOfBirth;

    @NotBlank(message = "Class name is required")
    private String className;

    private String guardianPhone;
    private String guardianEmail;
    private String address;

    @NotNull(message = "Admission date is required")
    private LocalDate admissionDate;

    private StudentStatus status;  // Can be null, defaults to ACTIVE

    @NotNull(message = "Monthly fee is required")
    @Min(value = 0, message = "Monthly fee cannot be negative")
    private Integer monthlyFee;

    private String photoUrl;
}