package com.mms.controller;

import com.mms.dto.request.BulkClassUpdateRequest;
import com.mms.dto.request.StudentRequest;
import com.mms.dto.response.PaginatedResponse;
import com.mms.entity.Student;
import com.mms.entity.StudentDocument;
import com.mms.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {
    private final StudentService studentService;

    // src/main/java/com/mms/controller/StudentController.java


    @GetMapping
    public ResponseEntity<PaginatedResponse<Student>> getStudents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String classroomId,  // This parameter exists
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        // Call service with all 6 parameters
        Page<Student> students = studentService.searchStudents(
                q,           // search
                className,   // className
                classroomId, // classroomId  - This must be passed!
                status,      // status
                page,        // page
                pageSize     // pageSize
        );

        return ResponseEntity.ok(PaginatedResponse.<Student>builder()
                .items(students.getContent())
                .total(students.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudent(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getStudent(id));
    }

    @PostMapping
    public ResponseEntity<Student> createStudent(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.createStudent(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable String id,
                                                 @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable String id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    // src/main/java/com/mms/controller/StudentController.java
// Add this method (or update existing)

    @PostMapping("/{id}/documents")
    public ResponseEntity<StudentDocument> uploadDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading document for student: {}, file: {}", id, file.getOriginalFilename());

        StudentDocument document = studentService.uploadDocument(id, file);
        log.info("Document uploaded successfully. URL: {}", document.getUrl());

        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }

    @DeleteMapping("/{id}/documents/{docId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id,
                                               @PathVariable String docId) {
        studentService.deleteDocument(id, docId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<?> getLedger(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getLedger(id));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getBalance(id));
    }
    // src/main/java/com/mms/controller/StudentController.java

    @PatchMapping("/bulk-class")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MUHTAMIM')")
    public ResponseEntity<Map<String, Object>> bulkUpdateClass(
            @RequestBody BulkClassUpdateRequest request) {
        int updated = studentService.bulkUpdateClass(request);
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
