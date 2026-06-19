package com.mms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyInvoiceResponse {
    private String month;
    private Integer inserted;
    private Integer skipped;
}
