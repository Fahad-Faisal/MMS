// src/main/java/com/mms/service/ReceiptService.java
package com.mms.service;

import com.mms.dto.request.ReceiptRequest;
import com.mms.entity.LedgerEntry;
import com.mms.entity.Receipt;
import com.mms.entity.Student;
import com.mms.enums.LedgerEntryType;
import com.mms.exception.ResourceNotFoundException;
import com.mms.repository.LedgerEntryRepository;
import com.mms.repository.ReceiptRepository;
import com.mms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    /**
     * Search receipts with filters
     */
    @Cacheable(value = "receipts", key = "{#q, #from, #to, #studentId, #page, #pageSize}")
    public Page<Receipt> searchReceipts(String q, LocalDate from, LocalDate to,
                                        String studentId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return receiptRepository.searchReceipts(q, from, to, studentId, pageable);
    }

    /**
     * Get receipt by ID
     */
    @Cacheable(value = "receipt", key = "#id")
    public Receipt getReceipt(String id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + id));
    }

    /**
     * Create a new receipt and corresponding ledger entry
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "receipts", allEntries = true),
            @CacheEvict(value = "receipt", key = "#result.id"),
            @CacheEvict(value = "ledger", key = "#request.studentId"),
            @CacheEvict(value = "balance", key = "#request.studentId"),
            @CacheEvict(value = "defaulters", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Receipt createReceipt(ReceiptRequest request) {
        log.info("Creating receipt for student: {}, amount: {}, method: {}",
                request.getStudentId(), request.getAmount(), request.getMethod());

        // Validate student exists
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

        // Generate receipt number
        long count = receiptRepository.countByPaidOnAfter(request.getPaidOn().withDayOfMonth(1));
        String year = String.valueOf(request.getPaidOn().getYear());
        String receiptNo = String.format("R-%s-%06d", year, count + 1);

        // Create receipt
        Receipt receipt = Receipt.builder()
                .receiptNo(receiptNo)
                .studentId(student.getId())
                .studentName(student.getFullName())
                .amount(request.getAmount())
                .method(request.getMethod())
                .paidOn(request.getPaidOn())
                .issuedBy(getCurrentUserName())
                .note(request.getNote())
                .build();

        Receipt saved = receiptRepository.save(receipt);
        log.info("Receipt created: {}", receiptNo);

        // Create corresponding ledger entry (payment)
        LedgerEntry payment = LedgerEntry.builder()
                .studentId(student.getId())
                .type(LedgerEntryType.PAYMENT)
                .amount(-request.getAmount())
                .description("Receipt " + receiptNo + (request.getNote() != null ? " — " + request.getNote() : ""))
                .referenceId(saved.getId())
                .createdBy(getCurrentUserName())
                .createdAt(LocalDateTime.now())
                .build();

        ledgerRepository.save(payment);
        log.info("Ledger entry created for receipt: {}", receiptNo);

        // Audit log
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "CREATE_RECEIPT", "receipt", saved.getId());

        return saved;
    }

    /**
     * Get receipt by receipt number
     */
    @Cacheable(value = "receipt", key = "#receiptNo")
    public Receipt getReceiptByNumber(String receiptNo) {
        return receiptRepository.findByReceiptNo(receiptNo)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with number: " + receiptNo));
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