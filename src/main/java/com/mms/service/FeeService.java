// src/main/java/com/mms/service/FeeService.java
package com.mms.service;

import com.mms.dto.request.MonthlyInvoiceRequest;
import com.mms.dto.response.DefaulterResponse;
import com.mms.entity.LedgerEntry;
import com.mms.entity.Student;
import com.mms.enums.LedgerEntryType;
import com.mms.repository.LedgerEntryRepository;
import com.mms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeeService {
    private final LedgerEntryRepository ledgerRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    @Transactional
    @CacheEvict(value = {"ledger", "balance", "defaulters", "dashboard"}, allEntries = true)
    public LedgerEntry addLedgerEntry(LedgerEntry entry) {
        entry.setCreatedAt(LocalDateTime.now());
        entry.setCreatedBy(getCurrentUserName());
        LedgerEntry saved = ledgerRepository.save(entry);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "CREATE_LEDGER_ENTRY", "ledger", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"ledger", "balance", "defaulters", "dashboard", "students"}, allEntries = true)
    public Map<String, Object> runMonthlyInvoices(MonthlyInvoiceRequest request) {
        String monthStr = request.getMonth() != null ? request.getMonth() :
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        String[] parts = monthStr.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        LocalDate monthDate = LocalDate.of(year, month, 1);
        String monthLabel = monthDate.getMonth().toString() + " " + year;

        List<Student> activeStudents = studentRepository.findByStatus(com.mms.enums.StudentStatus.ACTIVE);
        int inserted = 0;
        int skipped = 0;

        LocalDateTime start = monthDate.atStartOfDay();
        LocalDateTime end = monthDate.plusMonths(1).atStartOfDay().minusSeconds(1);

        for (Student student : activeStudents) {
            String description = monthLabel + " tuition";

            // Pass the enum directly, not a string
            boolean exists = ledgerRepository.existsByStudentIdAndDescriptionAndTypeAndCreatedAtBetween(
                    student.getId(),
                    description,
                    LedgerEntryType.INVOICE,  // Pass the enum directly
                    start,
                    end
            );

            if (exists) {
                skipped++;
                continue;
            }

            LedgerEntry invoice = LedgerEntry.builder()
                    .studentId(student.getId())
                    .type(LedgerEntryType.INVOICE)
                    .amount(student.getMonthlyFee())
                    .description(description)
                    .createdBy(getCurrentUserName())
                    .createdAt(monthDate.atStartOfDay())
                    .build();

            ledgerRepository.save(invoice);
            inserted++;
        }

        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "RUN_MONTHLY_INVOICES", "ledger", null);

        Map<String, Object> result = new HashMap<>();
        result.put("month", monthStr);
        result.put("inserted", inserted);
        result.put("skipped", skipped);
        return result;
    }

    @Cacheable(value = "defaulters")
    public List<DefaulterResponse> getDefaulters() {
        List<Student> students = studentRepository.findAll();
        List<DefaulterResponse> defaulters = new ArrayList<>();

        for (Student student : students) {
            if (student.getStatus() != com.mms.enums.StudentStatus.ACTIVE) continue;

            Integer balance = ledgerRepository.getBalance(student.getId());
            if (balance != null && balance > 0) {
                int months = Math.max(1, (int) Math.ceil((double) balance / student.getMonthlyFee()));
                defaulters.add(DefaulterResponse.builder()
                        .studentId(student.getId())
                        .studentName(student.getFullName())
                        .className(student.getClassName())
                        .guardianPhone(student.getGuardianPhone())
                        .overdueAmount(balance)
                        .monthsOverdue(months)
                        .build());
            }
        }

        defaulters.sort((a, b) -> b.getOverdueAmount().compareTo(a.getOverdueAmount()));
        return defaulters;
    }

    // Refresh defaulters cache every hour
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 3600000)
    @CacheEvict(value = "defaulters", allEntries = true)
    public void evictDefaultersCache() {
        // This will clear the defaulters cache every hour
    }

    private String getCurrentUserId() {
        return "system";
    }

    private String getCurrentUserName() {
        return "system";
    }
}