// src/main/java/com/mms/controller/AttendanceController.java
package com.mms.controller;

import com.mms.dto.request.AttendanceBatchRequest;
import com.mms.dto.request.AttendanceSyncRequest;
import com.mms.dto.response.ApiError;
import com.mms.dto.response.AttendanceSyncResponse;
import com.mms.entity.Attendance;
import com.mms.entity.Classroom;
import com.mms.entity.User;
import com.mms.enums.AttendanceSession;
import com.mms.enums.Role;
import com.mms.exception.ResourceNotFoundException;
import com.mms.exception.UnauthorizedException;
import com.mms.repository.ClassroomRepository;
import com.mms.repository.UserRepository;
import com.mms.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Slf4j
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;

    /**
     * GET /api/attendance
     * Get attendance records with optional filters
     *
     * Query Parameters:
     * - date: YYYY-MM-DD (optional)
     * - session: FAJR or AFTERNOON (optional)
     * - className: filter by class name (optional)
     * - classroomId: filter by specific classroom/section (optional)
     *
     * Authorization:
     * - INSTRUCTOR: Only sees their assigned classrooms
     * - MUHTAMIM & ADMINISTRATOR: Can see all classrooms
     */
    @GetMapping
    public ResponseEntity<?> getAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AttendanceSession session,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String classroomId) {

        try {
            log.debug("Getting attendance with filters - date: {}, session: {}, className: {}, classroomId: {}",
                    date, session, className, classroomId);

            // Get current user and check permissions
            User currentUser = getCurrentUser();

            // If instructor, filter by their assigned classrooms
            if (currentUser.getRole() == Role.INSTRUCTOR) {
                // If classroomId is provided, verify it's assigned to this instructor
                if (classroomId != null) {
                    Classroom classroom = classroomRepository.findById(classroomId)
                            .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

                    if (!classroom.getTeacherId().equals(currentUser.getId())) {
                        throw new UnauthorizedException("You can only view attendance for your assigned sections");
                    }
                } else {
                    // Get all classrooms assigned to this instructor
                    List<Classroom> assignedClassrooms = classroomRepository.findByTeacherId(currentUser.getId());

                    if (assignedClassrooms.isEmpty()) {
                        return ResponseEntity.ok(List.of()); // No assigned classrooms
                    }

                    // If no classroomId provided but instructor, we need to get attendance for all their classrooms
                    // We'll let the service handle this
                }
            }

            List<Attendance> attendance = attendanceService.getAttendance(date, session, className, classroomId);
            return ResponseEntity.ok(attendance);

        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .code("RESOURCE_NOT_FOUND")
                            .build());
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .code("FORBIDDEN")
                            .build());
        } catch (Exception e) {
            log.error("Error getting attendance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while fetching attendance")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * POST /api/attendance/batch
     * Save multiple attendance records at once (upsert)
     *
     * Request Body:
     * {
     *   "date": "2026-06-22",
     *   "session": "FAJR",
     *   "className": "Hifz - Advanced",
     *   "classroomId": "uuid", // optional
     *   "records": [
     *     { "studentId": "uuid", "status": "PRESENT", "note": "On time" },
     *     { "studentId": "uuid", "status": "ABSENT", "note": "Sick" }
     *   ]
     * }
     *
     * Authorization:
     * - INSTRUCTOR: Only for their assigned classrooms
     * - MUHTAMIM & ADMINISTRATOR: Can save for any classroom
     */
    @PostMapping("/batch")
    public ResponseEntity<?> saveAttendanceBatch(@Valid @RequestBody AttendanceBatchRequest request) {
        try {
            log.info("Saving attendance batch - date: {}, session: {}, classroomId: {}, records: {}",
                    request.getDate(), request.getSession(), request.getClassroomId(),
                    request.getRecords() != null ? request.getRecords().size() : 0);

            // Get current user and check permissions
            User currentUser = getCurrentUser();

            // If instructor, verify they have permission for this classroom
            if (currentUser.getRole() == Role.INSTRUCTOR) {
                if (request.getClassroomId() == null) {
                    throw new UnauthorizedException("Instructors must specify a classroom for attendance");
                }

                Classroom classroom = classroomRepository.findById(request.getClassroomId())
                        .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

                if (!classroom.getTeacherId().equals(currentUser.getId())) {
                    throw new UnauthorizedException("You can only record attendance for your assigned sections");
                }
            }

            Map<String, Integer> result = attendanceService.saveBatch(request);
            return ResponseEntity.ok(result);

        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .code("RESOURCE_NOT_FOUND")
                            .build());
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .code("FORBIDDEN")
                            .build());
        } catch (Exception e) {
            log.error("Error saving attendance batch", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while saving attendance")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * POST /api/attendance/sync
     * Sync offline attendance records (for mobile/PWA offline support)
     *
     * Request Body:
     * {
     *   "batches": [
     *     {
     *       "date": "2026-06-22",
     *       "session": "FAJR",
     *       "className": "Hifz - Advanced",
     *       "classroomId": "uuid",
     *       "records": [...],
     *       "queuedAt": "2026-06-22T10:30:00Z"
     *     }
     *   ]
     * }
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncAttendance(@Valid @RequestBody AttendanceSyncRequest request) {
        try {
            log.info("Syncing offline attendance - batches: {}",
                    request.getBatches() != null ? request.getBatches().size() : 0);

            // Get current user and check permissions
            User currentUser = getCurrentUser();

            // Verify instructor permissions for each batch
            if (currentUser.getRole() == Role.INSTRUCTOR) {
                for (var batch : request.getBatches()) {
                    if (batch.getClassroomId() == null) {
                        throw new UnauthorizedException("Instructors must specify a classroom for attendance sync");
                    }

                    Classroom classroom = classroomRepository.findById(batch.getClassroomId())
                            .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + batch.getClassroomId()));

                    if (!classroom.getTeacherId().equals(currentUser.getId())) {
                        throw new UnauthorizedException("You can only sync attendance for your assigned sections");
                    }
                }
            }

            AttendanceSyncResponse response = attendanceService.syncAttendance(request);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .code("RESOURCE_NOT_FOUND")
                            .build());
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .code("FORBIDDEN")
                            .build());
        } catch (Exception e) {
            log.error("Error syncing attendance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while syncing attendance")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * GET /api/attendance/classroom/{classroomId}
     * Get attendance for a specific classroom
     * Convenience endpoint for frontend
     */
    @GetMapping("/classroom/{classroomId}")
    public ResponseEntity<?> getAttendanceByClassroom(
            @PathVariable String classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AttendanceSession session) {

        try {
            log.debug("Getting attendance for classroom: {} date: {} session: {}", classroomId, date, session);

            // Verify classroom exists
            Classroom classroom = classroomRepository.findById(classroomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

            // Check permissions
            User currentUser = getCurrentUser();
            if (currentUser.getRole() == Role.INSTRUCTOR &&
                    !classroom.getTeacherId().equals(currentUser.getId())) {
                throw new UnauthorizedException("You can only view attendance for your assigned sections");
            }

            List<Attendance> attendance = attendanceService.getAttendance(date, session, null, classroomId);
            return ResponseEntity.ok(attendance);

        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .code("RESOURCE_NOT_FOUND")
                            .build());
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .code("FORBIDDEN")
                            .build());
        } catch (Exception e) {
            log.error("Error getting attendance by classroom", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while fetching attendance")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * GET /api/attendance/student/{studentId}
     * Get attendance history for a specific student
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getAttendanceByStudent(
            @PathVariable String studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        try {
            log.debug("Getting attendance for student: {} from: {} to: {}", studentId, from, to);

            // Check permissions - any authenticated user can view student attendance
            // But instructors can only view their students
            User currentUser = getCurrentUser();
            if (currentUser.getRole() == Role.INSTRUCTOR) {
                // Verify the student is in one of the instructor's classrooms
                List<Classroom> assignedClassrooms = classroomRepository.findByTeacherId(currentUser.getId());
                boolean authorized = false;
                for (Classroom classroom : assignedClassrooms) {
                    if (classroom.getStudentIds().contains(studentId)) {
                        authorized = true;
                        break;
                    }
                }
                if (!authorized) {
                    throw new UnauthorizedException("You can only view attendance for students in your sections");
                }
            }

            List<Attendance> attendance = attendanceService.getAttendanceByStudent(studentId, from, to);
            return ResponseEntity.ok(attendance);

        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .code("RESOURCE_NOT_FOUND")
                            .build());
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .code("FORBIDDEN")
                            .build());
        } catch (Exception e) {
            log.error("Error getting attendance by student", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while fetching attendance")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * GET /api/attendance/summary
     * Get attendance summary for a classroom or class
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getAttendanceSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String classroomId,
            @RequestParam(required = false) String className) {

        try {
            log.debug("Getting attendance summary - date: {}, classroomId: {}, className: {}",
                    date, classroomId, className);

            User currentUser = getCurrentUser();

            // If instructor, verify permissions
            if (currentUser.getRole() == Role.INSTRUCTOR) {
                if (classroomId != null) {
                    Classroom classroom = classroomRepository.findById(classroomId)
                            .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

                    if (!classroom.getTeacherId().equals(currentUser.getId())) {
                        throw new UnauthorizedException("You can only view attendance summary for your assigned sections");
                    }
                } else {
                    // If no classroomId, get summary for all assigned classrooms
                    List<Classroom> assignedClassrooms = classroomRepository.findByTeacherId(currentUser.getId());
                    if (assignedClassrooms.isEmpty()) {
                        return ResponseEntity.ok(Map.of());
                    }
                }
            }

            Map<String, Object> summary = attendanceService.getAttendanceSummary(date, classroomId, className);
            return ResponseEntity.ok(summary);

        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .code("RESOURCE_NOT_FOUND")
                            .build());
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .code("FORBIDDEN")
                            .build());
        } catch (Exception e) {
            log.error("Error getting attendance summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while fetching attendance summary")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * Helper method to get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        String userId = authentication.getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}