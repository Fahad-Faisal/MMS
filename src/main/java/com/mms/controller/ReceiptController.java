package com.mms.controller;

import com.mms.dto.request.ReceiptRequest;
import com.mms.dto.response.PaginatedResponse;
import com.mms.entity.Receipt;
import com.mms.service.ReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
public class ReceiptController {
    private final ReceiptService receiptService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<Receipt>> getReceipts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        Page<Receipt> receipts = receiptService.getReceipts(from, to, studentId, page, pageSize);
        return ResponseEntity.ok(PaginatedResponse.<Receipt>builder()
                .items(receipts.getContent())
                .total(receipts.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Receipt> getReceipt(@PathVariable String id) {
        return ResponseEntity.ok(receiptService.getReceipt(id));
    }

    @PostMapping
    public ResponseEntity<Receipt> createReceipt(@Valid @RequestBody ReceiptRequest request) {
        return ResponseEntity.ok(receiptService.createReceipt(request));
    }
}

