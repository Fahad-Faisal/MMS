package com.mms.controller;

import com.mms.dto.response.PaginatedResponse;
import com.mms.entity.AuditLog;
import com.mms.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        Page<AuditLog> logs = auditService.getAuditLogs(actor, entity, from, to, page, pageSize);
        return ResponseEntity.ok(PaginatedResponse.<AuditLog>builder()
                .items(logs.getContent())
                .total(logs.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build());
    }

    @PostMapping
    public ResponseEntity<AuditLog> createAuditLog(@RequestBody AuditLog log) {
        return ResponseEntity.ok(auditService.createManualAuditLog(log));
    }
}
