// src/main/java/com/mms/service/AttendanceService.java
package com.mms.service;

import com.mms.dto.request.AttendanceBatchRequest;
import com.mms.dto.request.AttendanceSyncRequest;
import com.mms.dto.response.AttendanceSyncResponse;
import com.mms.entity.Attendance;
import com.mms.entity.Classroom;
import com.mms.entity.Student;
import com.mms.enums.AttendanceSession;
import com.mms.enums.AttendanceStatus;
import com.mms.enums.StudentStatus;
import com.mms.exception.ResourceNotFoundException;
import com.mms.repository.AttendanceRepository;
import com.mms.repository.ClassroomRepository;
import com.mms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final AuditService auditService;

    /**
     * Get attendance records with filters
     * @param date - specific date (optional)
     * @param session - FAJR or AFTERNOON (optional)
     * @param className - filter by class name (optional)
     * @param classroomId - filter by specific classroom/section (optional)
     */
    @Cacheable(value = "attendance", key = "{#date, #session, #className, #classroomId}")
    public List<Attendance> getAttendance(LocalDate date, AttendanceSession session,
                                          String className, String classroomId) {
        log.debug("Getting attendance - date: {}, session: {}, className: {}, classroomId: {}",
                date, session, className, classroomId);

        // If classroomId is provided, use it directly
        if (classroomId != null && !classroomId.isEmpty()) {
            Classroom classroom = classroomRepository.findById(classroomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomId));

            List<String> studentIds = classroom.getStudentIds();
            if (studentIds.isEmpty()) {
                return new ArrayList<>();
            }

            if (date != null && session != null) {
                return attendanceRepository.findByDateAndSessionAndStudentIdIn(date, session, studentIds);
            } else if (date != null) {
                return attendanceRepository.findByDateAndStudentIdIn(date, studentIds);
            } else {
                return attendanceRepository.findByStudentIdIn(studentIds);
            }
        }

        // If className is provided, get students in that class
        if (className != null && !className.isEmpty()) {
            List<Student> students = studentRepository.findByStatus(StudentStatus.ACTIVE);
            List<String> studentIds = students.stream()
                    .filter(s -> className.equals(s.getClassName()))
                    .map(Student::getId)
                    .collect(Collectors.toList());

            if (studentIds.isEmpty()) {
                return new ArrayList<>();
            }

            if (date != null && session != null) {
                return attendanceRepository.findByDateAndSessionAndStudentIdIn(date, session, studentIds);
            } else if (date != null) {
                return attendanceRepository.findByDateAndStudentIdIn(date, studentIds);
            } else {
                return attendanceRepository.findByStudentIdIn(studentIds);
            }
        }

        // No specific filters - get all or by date/session
        if (date != null && session != null) {
            return attendanceRepository.findByDateAndSession(date, session);
        } else if (date != null) {
            return attendanceRepository.findByDate(date);
        } else if (session != null) {
            return attendanceRepository.findBySession(session);
        } else {
            return attendanceRepository.findAll();
        }
    }

    /**
     * Get attendance for a specific classroom (convenience method)
     */
    public List<Attendance> getAttendanceByClassroom(String classroomId, LocalDate date, AttendanceSession session) {
        return getAttendance(date, session, null, classroomId);
    }

    /**
     * Get attendance history for a specific student
     */
    public List<Attendance> getAttendanceByStudent(String studentId, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return attendanceRepository.findByStudentIdAndDateBetween(studentId, from, to);
        } else if (from != null) {
            return attendanceRepository.findByStudentIdAndDateAfter(studentId, from);
        } else if (to != null) {
            return attendanceRepository.findByStudentIdAndDateBefore(studentId, to);
        } else {
            return attendanceRepository.findByStudentIdOrderByDateDesc(studentId);
        }
    }

    /**
     * Get attendance summary for a classroom or class
     */
    public Map<String, Object> getAttendanceSummary(LocalDate date, String classroomId, String className) {
        Map<String, Object> summary = new HashMap<>();
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<Attendance> attendance;
        if (classroomId != null && !classroomId.isEmpty()) {
            Classroom classroom = classroomRepository.findById(classroomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
            attendance = attendanceRepository.findByDateAndStudentIdIn(
                    targetDate,
                    classroom.getStudentIds()
            );
        } else if (className != null && !className.isEmpty()) {
            List<Student> students = studentRepository.findByStatus(StudentStatus.ACTIVE);
            List<String> studentIds = students.stream()
                    .filter(s -> className.equals(s.getClassName()))
                    .map(Student::getId)
                    .collect(Collectors.toList());
            attendance = attendanceRepository.findByDateAndStudentIdIn(targetDate, studentIds);
        } else {
            attendance = attendanceRepository.findByDate(targetDate);
        }

        // Calculate statistics
        long total = attendance.size();
        long present = attendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .count();
        long absent = attendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .count();
        long late = attendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LATE)
                .count();
        long sick = attendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.SICK)
                .count();

        summary.put("date", targetDate.toString());
        summary.put("total", total);
        summary.put("present", present);
        summary.put("absent", absent);
        summary.put("late", late);
        summary.put("sick", sick);
        summary.put("attendanceRate", total > 0 ? (double) present / total * 100 : 0.0);

        return summary;
    }

    @Transactional
    @CacheEvict(value = {"attendance", "dashboard"}, allEntries = true)
    public Map<String, Integer> saveBatch(AttendanceBatchRequest request) {
        log.info("Saving attendance batch - date: {}, session: {}, classroomId: {}, records: {}",
                request.getDate(), request.getSession(), request.getClassroomId(),
                request.getRecords() != null ? request.getRecords().size() : 0);

        int inserted = 0;
        int updated = 0;

        // If classroomId is provided, verify all students belong to this classroom
        if (request.getClassroomId() != null && !request.getClassroomId().isEmpty()) {
            Classroom classroom = classroomRepository.findById(request.getClassroomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

            Set<String> validStudentIds = new HashSet<>(classroom.getStudentIds());
            for (AttendanceBatchRequest.AttendanceRecord record : request.getRecords()) {
                if (!validStudentIds.contains(record.getStudentId())) {
                    log.warn("Student {} is not in classroom {}", record.getStudentId(), request.getClassroomId());
                    // Skip or throw - we'll throw to maintain data integrity
                    throw new IllegalArgumentException(
                            "Student " + record.getStudentId() + " is not enrolled in this classroom"
                    );
                }
            }
        }

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
            attendance.setRecordedAt(LocalDateTime.now());

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

    @Transactional
    public AttendanceSyncResponse syncAttendance(AttendanceSyncRequest request) {
        log.info("Syncing attendance - batches: {}",
                request.getBatches() != null ? request.getBatches().size() : 0);

        List<AttendanceSyncResponse.SyncResult> results = new ArrayList<>();

        for (var batch : request.getBatches()) {
            // Verify classroom if provided
            if (batch.getClassroomId() != null && !batch.getClassroomId().isEmpty()) {
                Classroom classroom = classroomRepository.findById(batch.getClassroomId())
                        .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + batch.getClassroomId()));

                Set<String> validStudentIds = new HashSet<>(classroom.getStudentIds());
                for (var record : batch.getRecords()) {
                    if (!validStudentIds.contains(record.getStudentId())) {
                        results.add(AttendanceSyncResponse.SyncResult.builder()
                                .clientId(record.getClientId())
                                .success(false)
                                .message("Student not in classroom")
                                .build());
                        continue;
                    }

                    // Process valid records
                    try {
                        var existing = attendanceRepository.findByStudentIdAndDateAndSession(
                                record.getStudentId(), batch.getDate(), batch.getSession()
                        );

                        Attendance attendance = existing.orElse(Attendance.builder()
                                .studentId(record.getStudentId())
                                .date(batch.getDate())
                                .session(batch.getSession())
                                .build());

                        attendance.setStatus(record.getStatus());
                        attendance.setNote(record.getNote());
                        attendance.setRecordedBy(getCurrentUserName());
                        attendance.setRecordedAt(record.getRecordedAt() != null ?
                                record.getRecordedAt() : LocalDateTime.now());

                        Attendance saved = attendanceRepository.save(attendance);

                        results.add(AttendanceSyncResponse.SyncResult.builder()
                                .clientId(record.getClientId())
                                .success(true)
                                .serverId(saved.getId())
                                .build());

                    } catch (Exception e) {
                        log.error("Failed to sync attendance record", e);
                        results.add(AttendanceSyncResponse.SyncResult.builder()
                                .clientId(record.getClientId())
                                .success(false)
                                .message(e.getMessage())
                                .build());
                    }
                }
            } else {
                // No classroom validation - process all records
                for (var record : batch.getRecords()) {
                    try {
                        var existing = attendanceRepository.findByStudentIdAndDateAndSession(
                                record.getStudentId(), batch.getDate(), batch.getSession()
                        );

                        Attendance attendance = existing.orElse(Attendance.builder()
                                .studentId(record.getStudentId())
                                .date(batch.getDate())
                                .session(batch.getSession())
                                .build());

                        attendance.setStatus(record.getStatus());
                        attendance.setNote(record.getNote());
                        attendance.setRecordedBy(getCurrentUserName());
                        attendance.setRecordedAt(record.getRecordedAt() != null ?
                                record.getRecordedAt() : LocalDateTime.now());

                        Attendance saved = attendanceRepository.save(attendance);

                        results.add(AttendanceSyncResponse.SyncResult.builder()
                                .clientId(record.getClientId())
                                .success(true)
                                .serverId(saved.getId())
                                .build());

                    } catch (Exception e) {
                        log.error("Failed to sync attendance record", e);
                        results.add(AttendanceSyncResponse.SyncResult.builder()
                                .clientId(record.getClientId())
                                .success(false)
                                .message(e.getMessage())
                                .build());
                    }
                }
            }
        }

        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "SYNC_ATTENDANCE", "attendance", null);

        return AttendanceSyncResponse.builder()
                .results(results)
                .build();
    }

    private String getCurrentUserId() {
        // Implementation will get from SecurityContext
        return "system";
    }

    private String getCurrentUserName() {
        // Implementation will get from SecurityContext
        return "system";
    }
}