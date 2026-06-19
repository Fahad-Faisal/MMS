// src/main/java/com/mms/repository/LedgerEntryRepository.java
package com.mms.repository;

import com.mms.entity.LedgerEntry;
import com.mms.enums.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {
    List<LedgerEntry> findByStudentIdOrderByCreatedAtDesc(String studentId);

    @Query("SELECT SUM(l.amount) FROM LedgerEntry l WHERE l.studentId = :studentId")
    Integer getBalance(@Param("studentId") String studentId);

    // Fix: Change parameter type from String to LedgerEntryType
    boolean existsByStudentIdAndDescriptionAndTypeAndCreatedAtBetween(
            String studentId,
            String description,
            LedgerEntryType type,  // Changed from String to LedgerEntryType
            LocalDateTime start,
            LocalDateTime end
    );
}