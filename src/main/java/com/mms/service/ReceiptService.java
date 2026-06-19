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
public class ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    @Cacheable(value = "receipts", key = "{#from, #to, #studentId, #page, #pageSize}")
    public Page<Receipt> getReceipts(LocalDate from, LocalDate to, String studentId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return receiptRepository.findReceipts(from, to, studentId, pageable);
    }

    @Cacheable(value = "receipt", key = "#id")
    public Receipt getReceipt(String id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "receipts", allEntries = true),
            @CacheEvict(value = "ledger", key = "#request.studentId"),
            @CacheEvict(value = "balance", key = "#request.studentId"),
            @CacheEvict(value = "defaulters", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Receipt createReceipt(ReceiptRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        long count = receiptRepository.countByPaidOnAfter(request.getPaidOn().withDayOfMonth(1));
        String year = String.valueOf(request.getPaidOn().getYear());
        String receiptNo = String.format("R-%s-%06d", year, count + 1);

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
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "CREATE_RECEIPT", "receipt", saved.getId());

        return saved;
    }

    private String getCurrentUserId() {
        return "system";
    }

    private String getCurrentUserName() {
        return "system";
    }
}