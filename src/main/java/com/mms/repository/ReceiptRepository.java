// src/main/java/com/mms/repository/ReceiptRepository.java
package com.mms.repository;

import com.mms.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    @Query("SELECT r FROM Receipt r WHERE " +
            "(:from IS NULL OR r.paidOn >= :from) AND " +
            "(:to IS NULL OR r.paidOn <= :to) AND " +
            "(:studentId IS NULL OR r.studentId = :studentId)")
    Page<Receipt> findReceipts(@Param("from") LocalDate from,
                               @Param("to") LocalDate to,
                               @Param("studentId") String studentId,
                               Pageable pageable);

    List<Receipt> findTop6ByOrderByPaidOnDesc();

    // Fix: Get SUM of amounts, not COUNT
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Receipt r WHERE r.paidOn >= :startDate")
    Integer sumAmountByPaidOnAfter(@Param("startDate") LocalDate startDate);

    // Keep count method if needed elsewhere
    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.paidOn >= :startDate")
    long countByPaidOnAfter(@Param("startDate") LocalDate startDate);
}