package com.mms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BalanceResponse {
    private Integer balance;
    private LocalDateTime asOf;
}
