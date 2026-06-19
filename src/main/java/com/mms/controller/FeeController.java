package com.mms.controller;

import com.mms.dto.request.MonthlyInvoiceRequest;
import com.mms.entity.LedgerEntry;
import com.mms.service.FeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class FeeController {
    private final FeeService feeService;

    @PostMapping
    public ResponseEntity<LedgerEntry> addLedgerEntry(@RequestBody LedgerEntry entry) {
        return ResponseEntity.ok(feeService.addLedgerEntry(entry));
    }

    @PostMapping("/run-monthly-invoices")
    public ResponseEntity<Map<String, Object>> runMonthlyInvoices(
            @Valid @RequestBody MonthlyInvoiceRequest request) {
        return ResponseEntity.ok(feeService.runMonthlyInvoices(request));
    }
}
