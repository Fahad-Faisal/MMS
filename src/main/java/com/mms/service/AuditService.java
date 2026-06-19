// src/main/java/com/mms/service/AuditService.java
package com.mms.service;

import com.mms.entity.AuditLog;
import com.mms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void logAudit(String actorId, String actorName, String action, String entity, String entityId) {
        try {
            AuditLog log = AuditLog.builder()
                    .actorId(actorId)
                    .actorName(actorName)
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .at(LocalDateTime.now())
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to log audit: {}", e.getMessage(), e);
        }
    }

    public Page<AuditLog> getAuditLogs(String actor, String entity, LocalDate from, LocalDate to, int page, int pageSize) {
        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);

            LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
            LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay().minusSeconds(1) : null;

            log.debug("Searching audit logs with - actor: {}, entity: {}, from: {}, to: {}",
                    actor, entity, fromDateTime, toDateTime);

            return auditLogRepository.searchAuditLogs(actor, entity, fromDateTime, toDateTime, pageable);
        } catch (Exception e) {
            log.error("Failed to get audit logs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch audit logs", e);
        }
    }

    public AuditLog createManualAuditLog(AuditLog log) {
        log.setAt(LocalDateTime.now());
        return auditLogRepository.save(log);
    }
}