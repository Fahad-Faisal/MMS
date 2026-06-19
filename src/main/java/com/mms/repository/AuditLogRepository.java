// src/main/java/com/mms/repository/AuditLogRepository.java
package com.mms.repository;

import com.mms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String>, AuditLogRepositoryCustom {
}