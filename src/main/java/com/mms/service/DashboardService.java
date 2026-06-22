// src/main/java/com/mms/service/DashboardService.java
package com.mms.service;

import com.mms.dto.response.DashboardMetrics;
import com.mms.entity.Attendance;
import com.mms.entity.Receipt;
import com.mms.entity.Student;
import com.mms.enums.AttendanceSession;
import com.mms.enums.AttendanceStatus;
import com.mms.enums.StudentStatus;
import com.mms.repository.AttendanceRepository;
import com.mms.repository.ReceiptRepository;
import com.mms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ReceiptRepository receiptRepository;
    private final FeeService feeService;

    @Cacheable(value = "dashboard", key = "'metrics'")
    public DashboardMetrics getMetrics() {
        log.debug("Calculating dashboard metrics...");

        // Total students
        long totalStudents = studentRepository.count();
        log.debug("Total students: {}", totalStudents);

        // Today's attendance - fetch all sessions
        LocalDate today = LocalDate.now();
        List<Attendance> todayAttendance = attendanceRepository.findByDateAndSessionAndStudentIdIn(
                today, null, null
        );

        // If the above returns empty, try fetching by date and session individually
        if (todayAttendance.isEmpty()) {
            log.debug("No attendance found with null session, trying specific sessions...");
            List<Attendance> fajrAttendance = attendanceRepository.findByDateAndSession(today, AttendanceSession.FAJR);
            List<Attendance> afternoonAttendance = attendanceRepository.findByDateAndSession(today, AttendanceSession.AFTERNOON);
            todayAttendance.addAll(fajrAttendance);
            todayAttendance.addAll(afternoonAttendance);
        }

        long present = todayAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();

        double attendancePercent = 0;
        if (!todayAttendance.isEmpty()) {
            attendancePercent = (double) present / todayAttendance.size() * 100;
        }
        log.debug("Attendance - Total: {}, Present: {}, Percent: {}%",
                todayAttendance.size(), present, attendancePercent);

        // Monthly collection - FIXED: Use SUM instead of COUNT
        LocalDate monthStart = today.withDayOfMonth(1);
        int collection = receiptRepository.sumAmountByPaidOnAfter(monthStart);
        log.debug("Monthly collection: {} (from {})", collection, monthStart);

        // Expected collection
        List<Student> activeStudents = studentRepository.findByStatus(StudentStatus.ACTIVE);
        int expected = activeStudents.stream()
                .mapToInt(Student::getMonthlyFee)
                .sum();
        log.debug("Expected collection: {} from {} active students", expected, activeStudents.size());

        // Defaulter count
        long defaulterCount = feeService.getDefaulters().size();
        log.debug("Defaulter count: {}", defaulterCount);

        // Recent receipts
        List<Receipt> recentReceipts = receiptRepository.findTop6ByOrderByPaidOnDesc();
        log.debug("Recent receipts: {}", recentReceipts.size());

        return DashboardMetrics.builder()
                .totalStudents(totalStudents)
                .attendanceTodayPercent(attendancePercent)
                .collectionThisMonth(collection)
                .expectedThisMonth(expected)
                .defaulterCount(defaulterCount)
                .recentReceipts(recentReceipts)
                .build();
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @CacheEvict(value = "dashboard", allEntries = true)
    public void evictDashboardCache() {
        log.debug("Dashboard cache evicted");
    }
}