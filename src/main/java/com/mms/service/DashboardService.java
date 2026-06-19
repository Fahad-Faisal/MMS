// src/main/java/com/mms/service/DashboardService.java
package com.mms.service;

import com.mms.dto.response.DashboardMetrics;
import com.mms.entity.Attendance;
import com.mms.entity.Student;
import com.mms.enums.AttendanceStatus;
import com.mms.enums.StudentStatus;
import com.mms.repository.AttendanceRepository;
import com.mms.repository.ReceiptRepository;
import com.mms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ReceiptRepository receiptRepository;
    private final FeeService feeService;

    @Cacheable(value = "dashboard", key = "'metrics'")
    public DashboardMetrics getMetrics() {
        long totalStudents = studentRepository.count();

        LocalDate today = LocalDate.now();
        List<Attendance> todayAttendance = attendanceRepository.findByDateAndSessionAndStudentIdIn(
                today, null, null
        );
        long present = todayAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        double attendancePercent = todayAttendance.isEmpty() ? 0 :
                (double) present / todayAttendance.size() * 100;

        LocalDate monthStart = today.withDayOfMonth(1);
        long collection = receiptRepository.countByPaidOnAfter(monthStart);

        int expected = studentRepository.findByStatus(StudentStatus.ACTIVE).stream()
                .mapToInt(Student::getMonthlyFee)
                .sum();

        long defaulterCount = feeService.getDefaulters().size();

        return DashboardMetrics.builder()
                .totalStudents(totalStudents)
                .attendanceTodayPercent(attendancePercent)
                .collectionThisMonth((int) collection)
                .expectedThisMonth(expected)
                .defaulterCount(defaulterCount)
                .recentReceipts(receiptRepository.findTop6ByOrderByPaidOnDesc())
                .build();
    }

    // Refresh dashboard cache every 5 minutes
    @Scheduled(fixedRate = 300000)
    @CacheEvict(value = "dashboard", allEntries = true)
    public void evictDashboardCache() {
        // This will clear the dashboard cache every 5 minutes
    }
}