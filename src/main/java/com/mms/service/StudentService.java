// src/main/java/com/mms/service/StudentService.java
package com.mms.service;

import com.mms.dto.request.StudentRequest;
import com.mms.dto.response.BalanceResponse;
import com.mms.entity.LedgerEntry;
import com.mms.entity.Student;
import com.mms.entity.StudentDocument;
import com.mms.enums.StudentStatus;
import com.mms.exception.ResourceNotFoundException;
import com.mms.repository.LedgerEntryRepository;
import com.mms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final StorageService storageService;
    private final AuditService auditService;

    @Cacheable(value = "student", key = "#id")
    public Student getStudent(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    // src/main/java/com/mms/service/StudentService.java - Update searchStudents method

    @Cacheable(value = "students", key = "{#search, #className, #status, #page, #pageSize}")
    public Page<Student> searchStudents(String search, String className, String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        // Convert status string to enum if provided
        String statusValue = null;
        if (status != null && !status.isEmpty()) {
            try {
                StudentStatus.valueOf(status);
                statusValue = status;
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}", status);
            }
        }

        return studentRepository.searchStudents(search, className, statusValue, pageable);
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

        Student student = Student.builder()
                .rollNo(request.getRollNo())
                .fullName(request.getFullName())
                .fatherName(request.getFatherName())
                .motherName(request.getMotherName())
                .dateOfBirth(request.getDateOfBirth())
                .className(request.getClassName())
                .guardianPhone(request.getGuardianPhone())
                .guardianEmail(request.getGuardianEmail())
                .address(request.getAddress())
                .admissionDate(request.getAdmissionDate())
                .status(request.getStatus() != null ? request.getStatus() : StudentStatus.ACTIVE)
                .monthlyFee(request.getMonthlyFee())
                .photoUrl(request.getPhotoUrl())
                .build();

        Student saved = studentRepository.save(student);
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

        // Upload to Supabase Storage
        String url = storageService.uploadFile(file, "students", studentId);
        String storageKey = storageService.getStorageKey();



        StudentDocument document = StudentDocument.builder()
                .fileName(StringUtils.cleanPath(file.getOriginalFilename()))
                .mimeType(file.getContentType())
                .storageKey(storageKey)
                .url(url)
                .uploadedAt(LocalDateTime.now())
                .build();

        if (student.getDocuments() == null) {
            student.setDocuments(new ArrayList<>());
        }
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

    private String getCurrentUserId() {
        // Implementation will get from SecurityContext
        return "system";
    }

    private String getCurrentUserName() {
        // Implementation will get from SecurityContext
        return "system";
    }
}