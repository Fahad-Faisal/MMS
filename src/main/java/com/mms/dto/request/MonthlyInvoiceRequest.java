package com.mms.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MonthlyInvoiceRequest {
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Month must be in YYYY-MM format")
    private String month;
}