package com.mms.dto.response;

import com.mms.entity.Receipt;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardMetrics {
    private Long totalStudents;
    private Double attendanceTodayPercent;
    private Integer collectionThisMonth;
    private Integer expectedThisMonth;
    private Long defaulterCount;
    private List<Receipt> recentReceipts;
}
