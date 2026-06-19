package com.mms.controller;

import com.mms.dto.response.DefaulterResponse;
import com.mms.service.FeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/defaulters")
@RequiredArgsConstructor
public class DefaulterController {
    private final FeeService feeService;

    @GetMapping
    public ResponseEntity<List<DefaulterResponse>> getDefaulters() {
        return ResponseEntity.ok(feeService.getDefaulters());
    }
}
