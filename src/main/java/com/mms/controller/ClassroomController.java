// src/main/java/com/mms/controller/ClassroomController.java
package com.mms.controller;

import com.mms.dto.request.ClassroomRequest;
import com.mms.dto.response.ApiError;
import com.mms.entity.Classroom;
import com.mms.entity.Student;
import com.mms.service.ClassroomService;
import com.mms.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/classrooms")
@RequiredArgsConstructor
@Slf4j
public class ClassroomController {
    private final ClassroomService classroomService;
    private final StudentService studentService;  // Inject StudentService

    @GetMapping
    public ResponseEntity<List<Classroom>> getClassrooms() {
        return ResponseEntity.ok(classroomService.getAllClassrooms());
    }

    @PostMapping
    public ResponseEntity<Classroom> createClassroom(@Valid @RequestBody ClassroomRequest request) {
        return ResponseEntity.ok(classroomService.createClassroom(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Classroom> updateClassroom(@PathVariable String id,
                                                     @RequestBody ClassroomRequest request) {
        return ResponseEntity.ok(classroomService.updateClassroom(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassroom(@PathVariable String id) {
        classroomService.deleteClassroom(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/classrooms/{id}/students
     * Get students enrolled in a specific classroom
     */
    @GetMapping("/{id}/students")
    public ResponseEntity<?> getClassroomStudents(@PathVariable String id) {
        try {
            Classroom classroom = classroomService.getClassroom(id);
            List<Student> students = studentService.getStudentsByClassAndClassroom(
                    classroom.getClassName(), id
            );
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            log.error("Error getting classroom students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Error fetching classroom students")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * GET /api/classrooms/available-students
     * Get students available for a class (not enrolled in any section)
     * Query Param: className
     */
    @GetMapping("/available-students")
    public ResponseEntity<?> getAvailableStudents(
            @RequestParam String className,
            @RequestParam(required = false) String excludeClassroomId) {

        try {
            log.info("Getting available students for class: {}, exclude: {}", className, excludeClassroomId);

            List<Student> students;
            if (excludeClassroomId != null && !excludeClassroomId.isEmpty()) {
                students = studentService.getAvailableStudentsForClassExcludingClassroom(
                        className, excludeClassroomId
                );
            } else {
                students = studentService.getAvailableStudentsForClass(className);
            }

            return ResponseEntity.ok(students);

        } catch (Exception e) {
            log.error("Error getting available students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Error fetching available students")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * GET /api/classrooms/{id}/available-students
     * Get students available for this classroom (not enrolled in any other section)
     * Excludes students already in this classroom when editing
     */
    @GetMapping("/{id}/available-students")
    public ResponseEntity<?> getAvailableStudentsForClassroom(@PathVariable String id) {
        try {
            Classroom classroom = classroomService.getClassroom(id);

            // Get students in the same class who are not in any classroom
            // or are in this specific classroom (already enrolled)
            List<Student> availableStudents = studentService.getAvailableStudentsForClassExcludingClassroom(
                    classroom.getClassName(), id
            );

            // Also include students already in this classroom
            List<Student> existingStudents = studentService.getStudentsByClassAndClassroom(
                    classroom.getClassName(), id
            );

            // Combine and deduplicate
            availableStudents.addAll(existingStudents);

            return ResponseEntity.ok(availableStudents);

        } catch (Exception e) {
            log.error("Error getting available students for classroom", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Error fetching available students")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }
}