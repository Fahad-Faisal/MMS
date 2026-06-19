// src/main/java/com/mms/controller/UploadController.java
package com.mms.controller;

import com.mms.dto.response.UploadResponse;
import com.mms.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Slf4j
public class UploadController {
    private final StorageService storageService;

    @PostMapping("/image")
    public ResponseEntity<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("Received image upload request: {}", file.getOriginalFilename());

        try {
            UploadResponse response = storageService.uploadImage(file);
            log.info("Image upload successful. URL: {}", response.getUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Image upload failed", e);
            throw e;
        }
    }
}