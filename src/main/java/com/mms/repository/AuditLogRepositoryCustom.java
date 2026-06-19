// src/main/java/com/mms/repository/AuditLogRepositoryCustom.java
package com.mms.repository;

import com.mms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogRepositoryCustom {
    Page<AuditLog> searchAuditLogs(String actor, String entity, LocalDateTime from, LocalDateTime to, Pageable pageable);
}