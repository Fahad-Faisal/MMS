// src/main/java/com/mms/controller/ReceiptController.java
package com.mms.controller;

import com.mms.dto.request.ReceiptRequest;
import com.mms.dto.response.ApiError;
import com.mms.dto.response.PaginatedResponse;
import com.mms.entity.Receipt;
import com.mms.exception.ResourceNotFoundException;
import com.mms.service.ReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Slf4j
public class ReceiptController {
    private final ReceiptService receiptService;

    /**
     * GET /api/receipts
     * Get receipts with optional filters
     *
     * Query Parameters:
     * - q: Search by student name or receipt number (optional)
     * - from: Start date (YYYY-MM-DD)
     * - to: End date (YYYY-MM-DD)
     * - studentId: Filter by student ID (optional)
     * - page: Page number (default: 1)
     * - pageSize: Items per page (default: 50)
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<Receipt>> getReceipts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        log.debug("Getting receipts - q: {}, from: {}, to: {}, studentId: {}, page: {}, pageSize: {}",
                q, from, to, studentId, page, pageSize);

        var receipts = receiptService.searchReceipts(q, from, to, studentId, page, pageSize);

        return ResponseEntity.ok(PaginatedResponse.<Receipt>builder()
                .items(receipts.getContent())
                .total(receipts.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build());
    }

    /**
     * GET /api/receipts/{id}
     * Get a specific receipt by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getReceipt(@PathVariable String id) {
        try {
            log.debug("Getting receipt: {}", id);
            Receipt receipt = receiptService.getReceipt(id);
            return ResponseEntity.ok(receipt);
        } catch (ResourceNotFoundException e) {
            log.warn("Receipt not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .code("RESOURCE_NOT_FOUND")
                            .build());
        } catch (Exception e) {
            log.error("Error getting receipt: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while fetching the receipt")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * POST /api/receipts
     * Create a new receipt
     */
    @PostMapping
    public ResponseEntity<?> createReceipt(@Valid @RequestBody ReceiptRequest request) {
        try {
            log.info("Creating receipt for student: {}, amount: {}",
                    request.getStudentId(), request.getAmount());

            Receipt receipt = receiptService.createReceipt(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(receipt);

        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .code("RESOURCE_NOT_FOUND")
                            .build());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .code("INVALID_REQUEST")
                            .build());
        } catch (Exception e) {
            log.error("Error creating receipt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while creating the receipt")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * GET /api/receipts/student/{studentId}
     * Get all receipts for a specific student
     * Convenience endpoint
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getReceiptsByStudent(
            @PathVariable String studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        try {
            log.debug("Getting receipts for student: {}, from: {}, to: {}", studentId, from, to);

            var receipts = receiptService.searchReceipts(null, from, to, studentId, page, pageSize);

            return ResponseEntity.ok(PaginatedResponse.<Receipt>builder()
                    .items(receipts.getContent())
                    .total(receipts.getTotalElements())
                    .page(page)
                    .pageSize(pageSize)
                    .build());

        } catch (Exception e) {
            log.error("Error getting receipts for student: {}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while fetching receipts")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    /**
     * GET /api/receipts/search
     * Search receipts by query string
     * Convenience endpoint
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchReceipts(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        try {
            log.debug("Searching receipts - q: {}, page: {}, pageSize: {}", q, page, pageSize);

            var receipts = receiptService.searchReceipts(q, null, null, null, page, pageSize);

            return ResponseEntity.ok(PaginatedResponse.<Receipt>builder()
                    .items(receipts.getContent())
                    .total(receipts.getTotalElements())
                    .page(page)
                    .pageSize(pageSize)
                    .build());

        } catch (Exception e) {
            log.error("Error searching receipts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("An error occurred while searching receipts")
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }
}