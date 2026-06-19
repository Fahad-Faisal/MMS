// src/main/java/com/mms/service/AttendanceService.java
package com.mms.service;

import com.mms.dto.request.AttendanceBatchRequest;
import com.mms.entity.Attendance;
import com.mms.enums.AttendanceSession;
import com.mms.enums.AttendanceStatus;
import com.mms.repository.AttendanceRepository;
import com.mms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    @Cacheable(value = "attendance", key = "{#date, #session, #className}")
    public List<Attendance> getAttendance(LocalDate date, AttendanceSession session, String className) {
        if (className != null) {
            var students = studentRepository.findByStatus(com.mms.enums.StudentStatus.ACTIVE);
            var studentIds = students.stream()
                    .filter(s -> s.getClassName().equals(className))
                    .map(com.mms.entity.Student::getId)
                    .toList();

            if (date != null && session != null) {
                return attendanceRepository.findByDateAndSessionAndStudentIdIn(date, session, studentIds);
            }
        }

        if (date != null && session != null) {
            return attendanceRepository.findByDateAndSessionAndStudentIdIn(date, session, null);
        }

        return attendanceRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = {"attendance", "dashboard"}, allEntries = true)
    public Map<String, Integer> saveBatch(AttendanceBatchRequest request) {
        int inserted = 0;
        int updated = 0;

        for (AttendanceBatchRequest.AttendanceRecord record : request.getRecords()) {
            var existing = attendanceRepository.findByStudentIdAndDateAndSession(
                    record.getStudentId(), request.getDate(), request.getSession()
            );

            Attendance attendance = existing.orElse(Attendance.builder()
                    .studentId(record.getStudentId())
                    .date(request.getDate())
                    .session(request.getSession())
                    .build());

            attendance.setStatus(record.getStatus());
            attendance.setNote(record.getNote());
            attendance.setRecordedBy(getCurrentUserName());

            attendanceRepository.save(attendance);

            if (existing.isPresent()) {
                updated++;
            } else {
                inserted++;
            }
        }

        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "SAVE_ATTENDANCE", "attendance", null);

        Map<String, Integer> result = new HashMap<>();
        result.put("inserted", inserted);
        result.put("updated", updated);
        return result;
    }

    public Map<String, Object> syncAttendance(Map<String, Object> request) {
        return Map.of("synced", true);
    }

    private String getCurrentUserId() {
        return "system";
    }

    private String getCurrentUserName() {
        return "system";
    }
}