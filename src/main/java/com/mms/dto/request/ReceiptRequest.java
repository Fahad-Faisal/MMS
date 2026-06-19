// src/main/java/com/mms/dto/request/ReceiptRequest.java
package com.mms.dto.request;

import com.mms.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReceiptRequest {
    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Integer amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    @NotNull(message = "Payment date is required")
    private LocalDate paidOn;

    private String note;
}