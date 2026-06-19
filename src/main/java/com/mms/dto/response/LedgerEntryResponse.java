package com.mms.dto.response;

import com.mms.enums.LedgerEntryType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LedgerEntryResponse {
    private String id;
    private String studentId;
    private LedgerEntryType type;
    private Integer amount;
    private String description;
    private String referenceId;
    private String createdBy;
    private LocalDateTime createdAt;
}

