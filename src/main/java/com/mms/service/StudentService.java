// src/main/java/com/mms/service/StudentService.java
package com.mms.service;

import com.mms.dto.request.BulkClassUpdateRequest;
import com.mms.dto.request.StudentRequest;
import com.mms.dto.response.BalanceResponse;
import com.mms.entity.Classroom;
import com.mms.entity.LedgerEntry;
import com.mms.entity.Student;
import com.mms.entity.StudentDocument;
import com.mms.enums.StudentStatus;
import com.mms.exception.BusinessException;
import com.mms.exception.ResourceNotFoundException;
import com.mms.repository.ClassroomRepository;
import com.mms.repository.LedgerEntryRepository;
import com.mms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {
    private final StudentRepository studentRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final ClassroomRepository classroomRepository;
    private final StorageService storageService;
    private final AuditService auditService;
    private final CacheManager cacheManager;

    @Cacheable(value = "student", key = "#id")
    public Student getStudent(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        populateClassroomName(student);
        return student;
    }

    @Cacheable(value = "students", key = "{#search, #className, #classroomId, #status, #page, #pageSize}")
    public Page<Student> searchStudents(String search, String className, String classroomId,
                                        String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        StudentStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = StudentStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }

        Page<Student> students = studentRepository.searchStudents(
                search, className, classroomId, statusEnum, pageable
        );

        students.getContent().forEach(this::populateClassroomName);
        return students;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "students", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "defaulters", allEntries = true)
    })
    public Student createStudent(StudentRequest request) {
        if (studentRepository.existsByRollNo(request.getRollNo())) {
            throw new RuntimeException("Roll number already exists");
        }

        if (request.getClassroomId() != null) {
            validateClassroomAssignment(request.getClassroomId(), request.getClassName());
        }

        Student student = Student.builder()
                .rollNo(request.getRollNo())
                .fullName(request.getFullName())
                .fatherName(request.getFatherName())
                .motherName(request.getMotherName())
                .dateOfBirth(request.getDateOfBirth())
                .className(request.getClassName())
                .classroomId(request.getClassroomId())
                .guardianPhone(request.getGuardianPhone())
                .guardianEmail(request.getGuardianEmail())
                .address(request.getAddress())
                .admissionDate(request.getAdmissionDate())
                .status(request.getStatus() != null ? request.getStatus() : StudentStatus.ACTIVE)
                .monthlyFee(request.getMonthlyFee())
                .photoUrl(request.getPhotoUrl())
                .build();

        Student saved = studentRepository.save(student);

        if (request.getClassroomId() != null) {
            addStudentToClassroom(request.getClassroomId(), saved.getId());
        }

        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "CREATE_STUDENT", "student", saved.getId());
        return saved;
    }

    @Transactional
    @Caching(put = {
            @CachePut(value = "student", key = "#id")
    }, evict = {
            @CacheEvict(value = "students", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "defaulters", allEntries = true)
    })
    public Student updateStudent(String id, StudentRequest request) {
        Student student = getStudent(id);

        if (request.getRollNo() != null && !request.getRollNo().equals(student.getRollNo())) {
            if (studentRepository.existsByRollNo(request.getRollNo())) {
                throw new RuntimeException("Roll number already exists");
            }
            student.setRollNo(request.getRollNo());
        }

        if (request.getClassroomId() != null && !request.getClassroomId().equals(student.getClassroomId())) {
            String className = request.getClassName() != null ? request.getClassName() : student.getClassName();
            validateClassroomAssignment(request.getClassroomId(), className);

            if (student.getClassroomId() != null) {
                removeStudentFromClassroom(student.getClassroomId(), student.getId());
            }

            addStudentToClassroom(request.getClassroomId(), student.getId());
            student.setClassroomId(request.getClassroomId());
        }

        if (request.getFullName() != null) student.setFullName(request.getFullName());
        if (request.getFatherName() != null) student.setFatherName(request.getFatherName());
        if (request.getMotherName() != null) student.setMotherName(request.getMotherName());
        if (request.getDateOfBirth() != null) student.setDateOfBirth(request.getDateOfBirth());
        if (request.getClassName() != null) student.setClassName(request.getClassName());
        if (request.getGuardianPhone() != null) student.setGuardianPhone(request.getGuardianPhone());
        if (request.getGuardianEmail() != null) student.setGuardianEmail(request.getGuardianEmail());
        if (request.getAddress() != null) student.setAddress(request.getAddress());
        if (request.getStatus() != null) student.setStatus(request.getStatus());
        if (request.getMonthlyFee() != null) student.setMonthlyFee(request.getMonthlyFee());
        if (request.getPhotoUrl() != null) student.setPhotoUrl(request.getPhotoUrl());

        Student updated = studentRepository.save(student);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "UPDATE_STUDENT", "student", updated.getId());
        return updated;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "student", key = "#id"),
            @CacheEvict(value = "students", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "defaulters", allEntries = true)
    })
    public void deleteStudent(String id) {
        Student student = getStudent(id);

        if (student.getClassroomId() != null) {
            removeStudentFromClassroom(student.getClassroomId(), student.getId());
        }

        studentRepository.deleteById(id);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "DELETE_STUDENT", "student", id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "student", key = "#studentId"),
            @CacheEvict(value = "students", allEntries = true)
    })
    public StudentDocument uploadDocument(String studentId, MultipartFile file) {
        Student student = getStudent(studentId);
        String url = storageService.uploadFile(file, "documents", studentId);

        StudentDocument document = StudentDocument.builder()
                .fileName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .storageKey(storageService.getStorageKey())
                .url(url)
                .build();

        student.getDocuments().add(document);
        studentRepository.save(student);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "UPLOAD_DOCUMENT", "student", studentId);
        return document;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "student", key = "#studentId"),
            @CacheEvict(value = "students", allEntries = true)
    })
    public void deleteDocument(String studentId, String docId) {
        Student student = getStudent(studentId);
        student.getDocuments().removeIf(doc -> doc.getId().equals(docId));
        studentRepository.save(student);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "DELETE_DOCUMENT", "student", studentId);
    }

    @Cacheable(value = "ledger", key = "#studentId")
    public List<LedgerEntry> getLedger(String studentId) {
        return ledgerRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Cacheable(value = "balance", key = "#studentId")
    public BalanceResponse getBalance(String studentId) {
        Integer balance = ledgerRepository.getBalance(studentId);
        return BalanceResponse.builder()
                .balance(balance != null ? balance : 0)
                .asOf(LocalDateTime.now())
                .build();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "students", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "defaulters", allEntries = true)
    })
    public int bulkUpdateClass(BulkClassUpdateRequest request) {
        Classroom targetClassroom = null;
        if (request.getClassroomId() != null) {
            targetClassroom = classroomRepository.findById(request.getClassroomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target classroom not found"));

            if (!targetClassroom.getClassName().equals(request.getClassName())) {
                throw new BusinessException("Classroom's class must match target class");
            }

            if (targetClassroom.getCapacity() != null) {
                int totalStudents = targetClassroom.getStudentIds().size() + request.getStudentIds().size();
                if (totalStudents > targetClassroom.getCapacity()) {
                    throw new BusinessException(
                            String.format("Target classroom capacity exceeded. %d students would exceed capacity of %d",
                                    totalStudents, targetClassroom.getCapacity())
                    );
                }
            }
        }

        int updated = 0;
        for (String studentId : request.getStudentIds()) {
            Student student = getStudent(studentId);

            if (student.getClassroomId() != null) {
                removeStudentFromClassroom(student.getClassroomId(), student.getId());
            }

            student.setClassName(request.getClassName());
            student.setClassroomId(request.getClassroomId());
            studentRepository.save(student);

            if (request.getClassroomId() != null) {
                addStudentToClassroom(request.getClassroomId(), student.getId());
            }

            auditService.logAudit(
                    getCurrentUserId(),
                    getCurrentUserName(),
                    "STUDENT_PROMOTED",
                    "student",
                    studentId
            );

            updated++;
        }

        evictAllCaches();
        return updated;
    }
    /**
     * Get students available for a class (not enrolled in any section)
     */
    public List<Student> getAvailableStudentsForClass(String className) {
        log.info("Getting available students for class: {}", className);
        return studentRepository.findAvailableStudentsForClass(className, StudentStatus.ACTIVE);
    }

    /**
     * Get students available for a class excluding those already in a specific classroom
     * (Used when editing a classroom)
     */
    public List<Student> getAvailableStudentsForClassExcludingClassroom(String className, String classroomId) {
        log.info("Getting available students for class: {} excluding classroom: {}", className, classroomId);
        return studentRepository.findAvailableStudentsForClassExcludingClassroom(
                className, classroomId, StudentStatus.ACTIVE
        );
    }

    /**
     * Get students already enrolled in a specific classroom
     */
    public List<Student> getStudentsByClassAndClassroom(String className, String classroomId) {
        log.info("Getting students for class: {} and classroom: {}", className, classroomId);
        return studentRepository.findStudentsByClassAndClassroom(className, classroomId, StudentStatus.ACTIVE);
    }

    // Helper methods
    private void populateClassroomName(Student student) {
        if (student.getClassroomId() != null) {
            classroomRepository.findById(student.getClassroomId())
                    .ifPresent(classroom -> student.setClassroomName(classroom.getName()));
        }
    }

    private void validateClassroomAssignment(String classroomId, String className) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

        if (!classroom.getClassName().equals(className)) {
            throw new BusinessException("Student's class must match classroom's class");
        }

        if (classroom.getCapacity() != null && classroom.getStudentIds().size() >= classroom.getCapacity()) {
            throw new BusinessException("Classroom is at full capacity");
        }
    }

    private void addStudentToClassroom(String classroomId, String studentId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

        if (!classroom.getStudentIds().contains(studentId)) {
            classroom.getStudentIds().add(studentId);
            classroomRepository.save(classroom);
        }
    }

    private void removeStudentFromClassroom(String classroomId, String studentId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

        classroom.getStudentIds().remove(studentId);
        classroomRepository.save(classroom);
    }

    private void evictAllCaches() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                try {
                    cacheManager.getCache(cacheName).clear();
                    log.debug("Cleared cache: {}", cacheName);
                } catch (Exception e) {
                    log.warn("Could not clear cache: {}", cacheName, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to clear all caches", e);
        }
    }

    private String getCurrentUserId() {
        return "system";
    }

    private String getCurrentUserName() {
        return "system";
    }
}